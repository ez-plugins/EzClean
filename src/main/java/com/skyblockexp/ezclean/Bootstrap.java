package com.skyblockexp.ezclean;

import com.skyblockexp.ezclean.command.EzCleanCommand;
import com.skyblockexp.ezclean.update.SpigotUpdateChecker;
import com.skyblockexp.ezclean.config.CleanupSettings;
import com.skyblockexp.ezclean.config.EzCleanConfigurationLoader;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import com.skyblockexp.ezclean.manager.DeathChestManager;
import com.skyblockexp.ezclean.model.DeathChestSettings;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass;
import java.util.List;
import java.util.logging.Level;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

/**
 * Handles the bootstrap lifecycle of the EzClean plugin.
 * Manages plugin initialization, configuration loading, and cleanup.
 */
public final class Bootstrap {

    private static final int SPIGOT_RESOURCE_ID = 129782;

    private final EzCleanPlugin plugin;
    private final EzCleanConfigurationLoader configurationLoader;

    public Bootstrap(EzCleanPlugin plugin) {
        this.plugin = plugin;
        this.configurationLoader = new EzCleanConfigurationLoader(plugin);
    }

    /**
     * Called during plugin load phase.
     * Pre-registers WorldGuard flags if available.
     */
    public void onLoad() {
        Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuardPlugin == null) {
            return;
        }

        try {
            WorldGuardCleanupBypass.registerBypassFlag(plugin.getLogger());
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to pre-register the EzClean WorldGuard bypass flag; region exclusions will be unavailable.",
                    throwable);
        }
    }

    /**
     * Called during plugin enable phase.
     * Initializes all plugin components and services.
     */
    public void onEnable() {
        try {
            // Save default config.yml on first run
            plugin.saveDefaultConfig();

            // Setup economy integration
            setupEconomy();

            // Initialize WorldGuard bypass
            WorldGuardCleanupBypass worldGuardBypass = initializeWorldGuardBypass();
            Registry.setWorldGuardBypass(worldGuardBypass);

            // Load and apply configuration
            List<CleanupSettings> cleanupSettings = applyConfiguration(configurationLoader.loadConfiguration());

            // Initialize metrics and update checker
            new Metrics(plugin, 27736);
            if (plugin.getConfig().getBoolean("update-check.enabled", true)) {
                new SpigotUpdateChecker(plugin, SPIGOT_RESOURCE_ID).checkForUpdates();
            }

            // Log initialization summary
            plugin.getLogger().info(() -> String.format("EzClean enabled. Loaded %d cleanup profile(s).",
                    cleanupSettings.size()));
            for (CleanupSettings settings : cleanupSettings) {
                String worlds = String.join(", ", settings.getEnabledWorlds());
                plugin.getLogger().info(() -> String.format(" - %s: every %d minutes (worlds: %s)",
                        settings.getCleanerId(), settings.getCleanupIntervalMinutes(), worlds));
            }
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Failed to enable EzClean plugin", throwable);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Called during plugin disable phase.
     * Cleans up all resources and shuts down services.
     */
    public void onDisable() {
        EntityCleanupScheduler cleanupScheduler = Registry.getCleanupScheduler();
        if (cleanupScheduler != null) {
            cleanupScheduler.disable();
        }

        DeathChestManager deathChestManager = Registry.getDeathChestManager();
        if (deathChestManager != null) {
            deathChestManager.shutdown();
        }

        CleanupStatsTracker statsTracker = Registry.getStatsTracker();
        if (statsTracker != null) {
            statsTracker.shutdown();
        }

        // Clear all registry entries
        Registry.clear();

        plugin.getLogger().info("EzClean plugin has been disabled.");
    }

    /**
     * Reloads the plugin configuration.
     */
    public void reloadConfiguration() {
        List<CleanupSettings> cleanupSettings = applyConfiguration(configurationLoader.loadConfiguration());
        plugin.getLogger().info(() -> String.format("Reloaded EzClean configuration. Loaded %d cleanup profile(s).",
                cleanupSettings.size()));
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (registration == null) {
            plugin.getLogger().info("No Vault economy provider detected. Pay-to-cancel will be disabled.");
            Registry.setEconomy(null);
            return;
        }

        Economy provider = registration.getProvider();
        if (provider == null) {
            plugin.getLogger().warning("Vault returned a null economy provider. Pay-to-cancel will be disabled.");
            Registry.setEconomy(null);
            return;
        }

        Registry.setEconomy(provider);
        plugin.getLogger().info("Vault economy detected. Players can pay to cancel upcoming cleanups when enabled.");
    }

    private WorldGuardCleanupBypass initializeWorldGuardBypass() {
        Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuardPlugin == null || !worldGuardPlugin.isEnabled()) {
            return null;
        }

        try {
            WorldGuardCleanupBypass bypass = WorldGuardCleanupBypass.create(plugin.getLogger());
            plugin.getLogger().info(() -> String.format(
                    "WorldGuard detected; regions with the '%s' flag set to ALLOW will be skipped during cleanups.",
                    bypass.getFlagName()));
            return bypass;
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to initialize WorldGuard cleanup bypass support; continuing without region exclusions.",
                    throwable);
            return null;
        }
    }

    private void registerCommands() {
        EntityCleanupScheduler cleanupScheduler = Registry.getCleanupScheduler();
        if (cleanupScheduler == null) {
            return;
        }

        EzCleanCommand command = new EzCleanCommand(plugin, cleanupScheduler);
        PluginCommand pluginCommand = plugin.getCommand("ezclean");
        if (pluginCommand == null) {
            plugin.getLogger().warning("Failed to register /ezclean command; entry missing from plugin.yml.");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    private List<CleanupSettings> applyConfiguration(FileConfiguration newConfiguration) {
        Registry.setConfiguration(newConfiguration);

        List<CleanupSettings> cleanupSettings = CleanupSettings.fromConfiguration(newConfiguration, plugin.getLogger());
        DeathChestSettings deathChestSettings = DeathChestSettings.fromConfiguration(newConfiguration);

        // Disable existing scheduler if present
        EntityCleanupScheduler existingScheduler = Registry.getCleanupScheduler();
        if (existingScheduler != null) {
            existingScheduler.disable();
        }

        // Initialize or update stats tracker
        CleanupStatsTracker statsTracker = Registry.getStatsTracker();
        if (statsTracker == null) {
            statsTracker = new CleanupStatsTracker(plugin);
            Registry.setStatsTracker(statsTracker);
        }

        // Create and enable new scheduler
        EntityCleanupScheduler cleanupScheduler = new EntityCleanupScheduler(
                plugin, cleanupSettings, Registry.getWorldGuardBypass(), statsTracker);
        cleanupScheduler.enable();
        Registry.setCleanupScheduler(cleanupScheduler);

        // Initialize or update death chest manager
        DeathChestManager deathChestManager = Registry.getDeathChestManager();
        if (deathChestManager == null) {
            deathChestManager = new DeathChestManager(plugin, deathChestSettings);
            Registry.setDeathChestManager(deathChestManager);
        } else {
            deathChestManager.applySettings(deathChestSettings);
        }

        registerCommands();

        return cleanupSettings;
    }
}
