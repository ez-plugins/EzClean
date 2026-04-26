package com.skyblockexp.ezclean;

import com.skyblockexp.ezclean.command.EzCleanCommand;
import com.skyblockexp.ezclean.gui.SetupGUI;
import com.skyblockexp.ezclean.gui.SetupGUIListener;
import com.skyblockexp.ezclean.lang.LangProvider;
import com.skyblockexp.ezclean.update.SpigotUpdateChecker;
import com.skyblockexp.ezclean.update.UpdateNoticeListener;
import com.skyblockexp.ezclean.config.CleanupSettings;
import com.skyblockexp.ezclean.config.DiscordWebhookSettings;
import com.skyblockexp.ezclean.config.EzCleanConfigurationLoader;
import com.skyblockexp.ezclean.integration.DiscordWebhookService;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import com.skyblockexp.ezclean.manager.DeathChestManager;
import com.skyblockexp.ezclean.model.DeathChestSettings;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import com.skyblockexp.ezclean.storage.StorageService;
import com.skyblockexp.ezclean.integration.EzCleanExpansion;
import com.skyblockexp.ezclean.integration.EzCountdownIntegration;
import com.skyblockexp.ezclean.integration.PapiHook;
import com.skyblockexp.ezclean.integration.SparkHook;
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
    private SetupGUI setupGUI;

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

            // Load language provider
            String langCode = plugin.getConfig().getString("language", "en");
            Registry.setLang(LangProvider.load(plugin, langCode));

            // Setup economy integration
            setupEconomy();

            // Initialize WorldGuard bypass
            WorldGuardCleanupBypass worldGuardBypass = initializeWorldGuardBypass();
            Registry.setWorldGuardBypass(worldGuardBypass);

            // Initialize EzCountdown integration
            EzCountdownIntegration ezCountdownIntegration = initializeEzCountdownIntegration();
            Registry.setEzCountdownIntegration(ezCountdownIntegration);

            // Load and apply configuration (also initialises StorageService + StatsTracker)
            List<CleanupSettings> cleanupSettings = applyConfiguration(configurationLoader.loadConfiguration());

            // Initialize metrics and update checker
            new Metrics(plugin, 27736);
            if (plugin.getConfig().getBoolean("update-check.enabled", true)) {
                SpigotUpdateChecker updateChecker = new SpigotUpdateChecker(plugin, SPIGOT_RESOURCE_ID);
                updateChecker.checkForUpdates();
                plugin.getServer().getPluginManager().registerEvents(
                        new UpdateNoticeListener(updateChecker), plugin);
            }

            // Register PlaceholderAPI expansion when available
            if (PapiHook.isEnabled()) {
                new EzCleanExpansion(plugin).register();
                plugin.getLogger().info("Hooked into PlaceholderAPI — EzClean placeholders registered.");
            }

            // Initialise Discord webhook integration
            initializeDiscordWebhook();

            // Register setup GUI and its listener
            setupGUI = new SetupGUI(plugin);
            plugin.getServer().getPluginManager().registerEvents(new SetupGUIListener(setupGUI), plugin);

            // Log Spark integration status
            if (SparkHook.isEnabled()) {
                plugin.getLogger().info("Hooked into Spark — using Spark TPS/MSPT data; auto-profiler available.");
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

        EzCountdownIntegration ezCountdownIntegration = Registry.getEzCountdownIntegration();
        if (ezCountdownIntegration != null) {
            ezCountdownIntegration.shutdown();
        }

        DeathChestManager deathChestManager = Registry.getDeathChestManager();
        if (deathChestManager != null) {
            deathChestManager.shutdown();
        }

        CleanupStatsTracker statsTracker = Registry.getStatsTracker();
        if (statsTracker != null) {
            statsTracker.shutdown();
        }

        DiscordWebhookService discordWebhookService = Registry.getDiscordWebhookService();
        if (discordWebhookService != null) {
            discordWebhookService.shutdown();
        }

        // Clear all registry entries
        Registry.clear();

        plugin.getLogger().info("EzClean plugin has been disabled.");
    }

    /**
     * Reloads the plugin configuration.
     */
    public void reloadConfiguration() {
        plugin.reloadConfig();
        String langCode = plugin.getConfig().getString("language", "en");
        Registry.setLang(LangProvider.load(plugin, langCode));
        // Re-initialise Discord webhook in case the URL or toggle changed.
        initializeDiscordWebhook();
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

    private void initializeDiscordWebhook() {
        // Shut down any existing service before creating a new one (e.g., on reload).
        DiscordWebhookService existing = Registry.getDiscordWebhookService();
        if (existing != null) {
            existing.shutdown();
            Registry.setDiscordWebhookService(null);
        }

        DiscordWebhookSettings webhookSettings = DiscordWebhookSettings.load(plugin.getConfig());
        if (webhookSettings == null) {
            return;
        }

        DiscordWebhookService service = new DiscordWebhookService(webhookSettings, plugin.getLogger());
        Registry.setDiscordWebhookService(service);
        plugin.getLogger().info("Discord webhook integration enabled.");
    }

    private EzCountdownIntegration initializeEzCountdownIntegration() {
        Plugin ezCountdownPlugin = plugin.getServer().getPluginManager().getPlugin("EzCountdown");
        if (ezCountdownPlugin == null || !ezCountdownPlugin.isEnabled()) {
            return null;
        }
        try {
            EzCountdownIntegration integration = EzCountdownIntegration.create(plugin.getLogger());
            if (integration != null) {
                plugin.getLogger().info(
                        "EzCountdown detected; cleanup timers will be mirrored to EzCountdown displays "
                                + "for profiles that have integrations.ezcountdown.enabled set to true.");
            }
            return integration;
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to initialize EzCountdown integration; continuing without EzCountdown support.",
                    throwable);
            return null;
        }
    }

    private void registerCommands() {
        EntityCleanupScheduler cleanupScheduler = Registry.getCleanupScheduler();
        if (cleanupScheduler == null) {
            return;
        }

        EzCleanCommand command = new EzCleanCommand(plugin, cleanupScheduler, setupGUI);
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

        // Initialise (or reuse) the storage service on first call.
        if (Registry.getStorageService() == null) {
            try {
                StorageService storageService = StorageService.create(
                        newConfiguration, plugin.getDataFolder(), plugin.getLogger());
                Registry.setStorageService(storageService);
            } catch (com.skyblockexp.ezclean.storage.StorageException ex) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to initialise EzClean storage — statistics will not be persisted.", ex);
            }
        }

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
            StorageService storageService = Registry.getStorageService();
            if (storageService != null) {
                statsTracker = new CleanupStatsTracker(plugin, storageService);
            } else {
                plugin.getLogger().warning(
                        "StorageService is unavailable; cleanup statistics will not be recorded.");
            }
            Registry.setStatsTracker(statsTracker);
        }

        // Create and enable new scheduler
        EntityCleanupScheduler cleanupScheduler = new EntityCleanupScheduler(
                plugin, cleanupSettings, Registry.getWorldGuardBypass(), statsTracker,
                Registry.getEzCountdownIntegration());
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
