package com.skyblockexp.ezclean.integration;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow.MillisPerTick;
import me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * Thin wrapper around the <a href="https://spark.lucko.me/">Spark</a> profiler API.
 *
 * <p>All methods are safe to call even when Spark is not installed — they return
 * {@code null} / perform no action in that case.</p>
 *
 * <p>Spark integration gives EzClean access to higher-quality TPS/MSPT rolling
 * averages and allows it to automatically start a Spark profiler session when an
 * unusually large entity count is detected, making it easier to identify the root
 * source of entity buildup without manual intervention.</p>
 */
public final class SparkHook {

    private SparkHook() {}

    /**
     * Returns {@code true} when the Spark plugin is present and enabled.
     */
    public static boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("spark");
    }

    /**
     * Returns the 1-minute rolling TPS average from Spark, or {@code null} when Spark
     * is not installed or the TPS statistic is unavailable on this platform.
     *
     * @return the 1-minute TPS average, or {@code null}
     */
    public static @Nullable Double getTps() {
        if (!isEnabled()) {
            return null;
        }
        try {
            Spark spark = SparkProvider.get();
            DoubleStatistic<TicksPerSecond> tps = spark.tps();
            if (tps != null) {
                double value = tps.poll(TicksPerSecond.MINUTES_1);
                return Double.isNaN(value) ? null : value;
            }
        } catch (Exception ignored) {
            // Spark not fully initialised or stat unavailable — fall through to null.
        }
        return null;
    }

    /**
     * Returns the mean 1-minute MSPT value from Spark, or {@code null} when Spark is
     * not installed or the MSPT statistic is unavailable on this platform.
     *
     * @return the mean 1-minute MSPT, or {@code null}
     */
    public static @Nullable Double getMspt() {
        if (!isEnabled()) {
            return null;
        }
        try {
            Spark spark = SparkProvider.get();
            GenericStatistic<DoubleAverageInfo, MillisPerTick> mspt = spark.mspt();
            if (mspt != null) {
                DoubleAverageInfo info = mspt.poll(MillisPerTick.MINUTES_1);
                double value = info.mean();
                return Double.isNaN(value) ? null : value;
            }
        } catch (Exception ignored) {
            // Spark not fully initialised or stat unavailable — fall through to null.
        }
        return null;
    }

    /**
     * Spawns a Spark profiler session via a console command dispatch.  The session
     * runs for {@code durationSeconds} seconds, then automatically stops and uploads
     * a report. The link is printed to the console so server staff can share it when
     * investigating entity buildup.
     *
     * <p>This method must be called from the main server thread (or the global region
     * thread on Folia), as {@link Bukkit#dispatchCommand} is not thread-safe.</p>
     *
     * @param plugin          the calling plugin, used for log output
     * @param durationSeconds how long to profile; clamped to a minimum of 5 seconds
     */
    public static void triggerAutoProfiler(JavaPlugin plugin, int durationSeconds) {
        if (!isEnabled()) {
            return;
        }
        int duration = Math.max(5, durationSeconds);
        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "spark profiler start --order-by-count --timeout " + duration);
        plugin.getLogger().info("[Spark] Auto-profiler started (" + duration + "s) — "
                + "high entity count detected. Report link will appear in console when complete.");
    }
}
