package com.skyblockexp.ezclean.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Scheduler adapter that works on both Paper/Spigot and Folia servers.
 *
 * <p>Folia introduces a regionized threading model where entity operations must
 * be performed on the thread that owns the entity's chunk region. This class
 * detects Folia at startup and delegates all scheduling calls to the appropriate
 * API, so the rest of the codebase remains scheduler-agnostic.
 */
public final class FoliaScheduler {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            folia = false;
        }
        FOLIA = folia;
    }

    private FoliaScheduler() {}

    /**
     * Returns {@code true} if the server is running Folia.
     */
    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Schedules a delayed one-shot task.
     *
     * <p>On Folia the task is submitted to the global region scheduler, which is
     * safe for Bukkit API calls that do not require a specific chunk region (e.g.,
     * world iteration or stats recording). On Paper/Spigot the task is submitted
     * to the main-thread BukkitScheduler.
     *
     * @param plugin     owning plugin
     * @param task       runnable to execute after the delay
     * @param delayTicks number of server ticks to wait
     * @return a {@link Runnable} that, when called, cancels the task (no-op if already run)
     */
    public static Runnable runGlobalLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (FOLIA) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduled =
                    Bukkit.getServer().getGlobalRegionScheduler()
                            .runDelayed(plugin, t -> task.run(), delayTicks);
            return scheduled::cancel;
        } else {
            BukkitTask scheduled = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return scheduled::cancel;
        }
    }

    /**
     * Schedules a repeating task on the global region thread (Folia) or the
     * main thread (Paper/Spigot).
     *
     * @param plugin             owning plugin
     * @param task               runnable to execute each period
     * @param initialDelayTicks  ticks before the first execution
     * @param periodTicks        ticks between subsequent executions
     * @return a {@link Runnable} that, when called, cancels the repeating task
     */
    public static Runnable runGlobalTimer(
            JavaPlugin plugin, Runnable task, long initialDelayTicks, long periodTicks) {
        if (FOLIA) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduled =
                    Bukkit.getServer().getGlobalRegionScheduler()
                            .runAtFixedRate(plugin, t -> task.run(), initialDelayTicks, periodTicks);
            return scheduled::cancel;
        } else {
            BukkitTask scheduled =
                    Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
            return scheduled::cancel;
        }
    }

    /**
     * Schedules an asynchronous background task.
     *
     * @param plugin owning plugin
     * @param task   runnable to execute on a background thread
     */
    public static void runAsync(JavaPlugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getServer().getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Removes an entity in a scheduler-safe manner.
     *
     * <p>On Folia, entity removal must occur on the thread that owns the entity's
     * chunk region. This method dispatches the removal to the entity's own
     * scheduler when running on Folia. On Paper/Spigot the entity is removed
     * immediately on the calling thread (which is assumed to be the main thread).
     *
     * @param entity entity to remove
     * @param plugin owning plugin (required by the entity scheduler on Folia)
     */
    public static void removeEntity(Entity entity, JavaPlugin plugin) {
        if (FOLIA) {
            entity.getScheduler().run(plugin, t -> entity.remove(), null);
        } else {
            entity.remove();
        }
    }
}
