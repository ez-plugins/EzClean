package com.skyblockexp.ezclean;

import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import com.skyblockexp.ezclean.manager.DeathChestManager;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass;
import org.bukkit.configuration.file.FileConfiguration;
import net.milkbowl.vault.economy.Economy;
import org.jetbrains.annotations.Nullable;

/**
 * Central registry for accessing EzClean plugin services and components.
 * Provides static access to shared dependencies throughout the plugin.
 */
public final class Registry {

    private static @Nullable Economy economy;
    private static @Nullable CleanupStatsTracker statsTracker;
    private static @Nullable EntityCleanupScheduler cleanupScheduler;
    private static @Nullable DeathChestManager deathChestManager;
    private static @Nullable WorldGuardCleanupBypass worldGuardBypass;
    private static @Nullable FileConfiguration configuration;

    // Prevent instantiation
    private Registry() {}

    /**
     * Gets the economy provider if available.
     */
    public static @Nullable Economy getEconomy() {
        return economy;
    }

    /**
     * Sets the economy provider.
     */
    public static void setEconomy(@Nullable Economy economy) {
        Registry.economy = economy;
    }

    /**
     * Gets the cleanup statistics tracker.
     */
    public static @Nullable CleanupStatsTracker getStatsTracker() {
        return statsTracker;
    }

    /**
     * Sets the cleanup statistics tracker.
     */
    public static void setStatsTracker(@Nullable CleanupStatsTracker statsTracker) {
        Registry.statsTracker = statsTracker;
    }

    /**
     * Gets the entity cleanup scheduler.
     */
    public static @Nullable EntityCleanupScheduler getCleanupScheduler() {
        return cleanupScheduler;
    }

    /**
     * Sets the entity cleanup scheduler.
     */
    public static void setCleanupScheduler(@Nullable EntityCleanupScheduler cleanupScheduler) {
        Registry.cleanupScheduler = cleanupScheduler;
    }

    /**
     * Gets the death chest manager.
     */
    public static @Nullable DeathChestManager getDeathChestManager() {
        return deathChestManager;
    }

    /**
     * Sets the death chest manager.
     */
    public static void setDeathChestManager(@Nullable DeathChestManager deathChestManager) {
        Registry.deathChestManager = deathChestManager;
    }

    /**
     * Gets the WorldGuard cleanup bypass handler.
     */
    public static @Nullable WorldGuardCleanupBypass getWorldGuardBypass() {
        return worldGuardBypass;
    }

    /**
     * Sets the WorldGuard cleanup bypass handler.
     */
    public static void setWorldGuardBypass(@Nullable WorldGuardCleanupBypass worldGuardBypass) {
        Registry.worldGuardBypass = worldGuardBypass;
    }

    /**
     * Gets the plugin configuration.
     */
    public static @Nullable FileConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the plugin configuration.
     */
    public static void setConfiguration(@Nullable FileConfiguration configuration) {
        Registry.configuration = configuration;
    }

    /**
     * Clears all registered services. Called during plugin disable.
     */
    public static void clear() {
        economy = null;
        statsTracker = null;
        cleanupScheduler = null;
        deathChestManager = null;
        worldGuardBypass = null;
        configuration = null;
    }
}
