package com.skyblockexp.ezclean;

import com.skyblockexp.ezclean.lang.LangProvider;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import com.skyblockexp.ezclean.manager.DeathChestManager;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import com.skyblockexp.ezclean.storage.StorageService;
import com.skyblockexp.ezclean.integration.EzCountdownIntegration;
import com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass;
import com.skyblockexp.ezclean.integration.DiscordWebhookService;
import org.bukkit.configuration.file.FileConfiguration;
import net.milkbowl.vault.economy.Economy;
import org.jetbrains.annotations.Nullable;

/**
 * Central registry for accessing EzClean plugin services and components.
 * Provides static access to shared dependencies throughout the plugin.
 */
public final class Registry {

    private static LangProvider lang = LangProvider.EMPTY;
    private static @Nullable Economy economy;
    private static @Nullable StorageService storageService;
    private static @Nullable CleanupStatsTracker statsTracker;
    private static @Nullable EntityCleanupScheduler cleanupScheduler;
    private static @Nullable DeathChestManager deathChestManager;
    private static @Nullable WorldGuardCleanupBypass worldGuardBypass;
    private static @Nullable EzCountdownIntegration ezCountdownIntegration;
    private static @Nullable FileConfiguration configuration;
    private static @Nullable DiscordWebhookService discordWebhookService;

    // Prevent instantiation
    private Registry() {}

    /** Returns the active language provider. Never null. */
    public static LangProvider getLang() {
        return lang;
    }

    /** Sets the active language provider. */
    public static void setLang(LangProvider provider) {
        Registry.lang = provider != null ? provider : LangProvider.EMPTY;
    }

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
     * Gets the storage service.
     */
    public static @Nullable StorageService getStorageService() {
        return storageService;
    }

    /**
     * Sets the storage service.
     */
    public static void setStorageService(@Nullable StorageService storageService) {
        Registry.storageService = storageService;
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
     * Gets the EzCountdown integration handler.
     */
    public static @Nullable EzCountdownIntegration getEzCountdownIntegration() {
        return ezCountdownIntegration;
    }

    /**
     * Sets the EzCountdown integration handler.
     */
    public static void setEzCountdownIntegration(@Nullable EzCountdownIntegration ezCountdownIntegration) {
        Registry.ezCountdownIntegration = ezCountdownIntegration;
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
     * Gets the Discord webhook service.
     */
    public static @Nullable DiscordWebhookService getDiscordWebhookService() {
        return discordWebhookService;
    }

    /**
     * Sets the Discord webhook service.
     */
    public static void setDiscordWebhookService(@Nullable DiscordWebhookService discordWebhookService) {
        Registry.discordWebhookService = discordWebhookService;
    }

    /**
     * Clears all registered services. Called during plugin disable.
     */
    public static void clear() {
        lang = LangProvider.EMPTY;
        economy = null;
        storageService = null;
        statsTracker = null;
        cleanupScheduler = null;
        deathChestManager = null;
        worldGuardBypass = null;
        ezCountdownIntegration = null;
        configuration = null;
        discordWebhookService = null;
    }
}
