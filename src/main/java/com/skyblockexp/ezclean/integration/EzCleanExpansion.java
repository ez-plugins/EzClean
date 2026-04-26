package com.skyblockexp.ezclean.integration;

import com.skyblockexp.ezclean.Registry;
import com.skyblockexp.ezclean.command.Msg;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker.CleanupStatsSnapshot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Exposes EzClean data as PlaceholderAPI placeholders.
 *
 * <p>All placeholders use the identifier {@code ezclean}. Cleaner-specific
 * placeholders accept an optional {@code _<cleanerId>} suffix; when the suffix
 * is omitted the first registered cleaner is used as the default.</p>
 *
 * <p><strong>Available placeholders:</strong></p>
 * <table>
 *   <caption>EzClean PlaceholderAPI placeholders</caption>
 *   <tr><th>Placeholder</th><th>Description</th></tr>
 *   <tr><td>%ezclean_next%</td><td>Time until next cleanup (first cleaner)</td></tr>
 *   <tr><td>%ezclean_next_&lt;id&gt;%</td><td>Time until next cleanup for the named cleaner</td></tr>
 *   <tr><td>%ezclean_last_removed%</td><td>Entities removed in the last run (first cleaner)</td></tr>
 *   <tr><td>%ezclean_last_removed_&lt;id&gt;%</td><td>Last-run count for the named cleaner</td></tr>
 *   <tr><td>%ezclean_total_removed%</td><td>All-time entities removed (first cleaner)</td></tr>
 *   <tr><td>%ezclean_total_removed_&lt;id&gt;%</td><td>All-time count for the named cleaner</td></tr>
 *   <tr><td>%ezclean_total_runs%</td><td>Total cleanup runs (first cleaner)</td></tr>
 *   <tr><td>%ezclean_total_runs_&lt;id&gt;%</td><td>Total runs for the named cleaner</td></tr>
 *   <tr><td>%ezclean_cleaners%</td><td>Comma-separated list of cleaner IDs</td></tr>
 * </table>
 */
public final class EzCleanExpansion extends PlaceholderExpansion {

    private static final String PLACEHOLDER_UNKNOWN = "";
    private static final String PLACEHOLDER_NA = "N/A";
    private static final String PLACEHOLDER_NONE = "none";

    private final JavaPlugin plugin;

    public EzCleanExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ezclean";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    /** The expansion persists across plugin reloads because it re-queries Registry on every request. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        // %ezclean_cleaners%
        if (params.equals("cleaners")) {
            EntityCleanupScheduler scheduler = Registry.getCleanupScheduler();
            if (scheduler == null) {
                return PLACEHOLDER_NONE;
            }
            List<String> ids = scheduler.getCleanerIds();
            return ids.isEmpty() ? PLACEHOLDER_NONE : String.join(", ", ids);
        }

        // %ezclean_next%, %ezclean_next_<id>%
        if (params.equals("next") || params.startsWith("next_")) {
            String cleanerId = params.equals("next") ? defaultCleanerId() : params.substring(5);
            if (cleanerId == null) {
                return PLACEHOLDER_NA;
            }
            EntityCleanupScheduler scheduler = Registry.getCleanupScheduler();
            if (scheduler == null) {
                return PLACEHOLDER_NA;
            }
            Duration remaining = scheduler.getTimeUntilCleanup(cleanerId);
            return remaining != null ? Msg.formatDuration(remaining) : PLACEHOLDER_NA;
        }

        // %ezclean_last_removed%, %ezclean_last_removed_<id>%
        if (params.equals("last_removed") || params.startsWith("last_removed_")) {
            String cleanerId = params.equals("last_removed") ? defaultCleanerId()
                    : params.substring("last_removed_".length());
            CleanupStatsSnapshot snapshot = getSnapshot(cleanerId);
            if (snapshot == null || snapshot.lastRun() == null) {
                return PLACEHOLDER_NA;
            }
            return Long.toString(snapshot.lastRun().removed());
        }

        // %ezclean_total_removed%, %ezclean_total_removed_<id>%
        if (params.equals("total_removed") || params.startsWith("total_removed_")) {
            String cleanerId = params.equals("total_removed") ? defaultCleanerId()
                    : params.substring("total_removed_".length());
            CleanupStatsSnapshot snapshot = getSnapshot(cleanerId);
            if (snapshot == null) {
                return PLACEHOLDER_NA;
            }
            return Long.toString(snapshot.totals().removed());
        }

        // %ezclean_total_runs%, %ezclean_total_runs_<id>%
        if (params.equals("total_runs") || params.startsWith("total_runs_")) {
            String cleanerId = params.equals("total_runs") ? defaultCleanerId()
                    : params.substring("total_runs_".length());
            CleanupStatsSnapshot snapshot = getSnapshot(cleanerId);
            if (snapshot == null) {
                return PLACEHOLDER_NA;
            }
            return Long.toString(snapshot.totals().runs());
        }

        return PLACEHOLDER_UNKNOWN;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Returns the first registered cleaner ID, or {@code null} when no scheduler is active. */
    private static @Nullable String defaultCleanerId() {
        EntityCleanupScheduler scheduler = Registry.getCleanupScheduler();
        if (scheduler == null) {
            return null;
        }
        List<String> ids = scheduler.getCleanerIds();
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static @Nullable CleanupStatsSnapshot getSnapshot(@Nullable String cleanerId) {
        if (cleanerId == null) {
            return null;
        }
        CleanupStatsTracker tracker = Registry.getStatsTracker();
        if (tracker == null) {
            return null;
        }
        return tracker.getSnapshot(cleanerId.toLowerCase(Locale.ROOT));
    }
}
