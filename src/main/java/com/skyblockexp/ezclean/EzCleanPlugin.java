package com.skyblockexp.ezclean;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point for the EzClean plugin that automates periodic entity cleanup tasks.
 */
public final class EzCleanPlugin extends JavaPlugin {

    private Bootstrap bootstrap;
    private FileConfiguration configuration = new YamlConfiguration();

    @Override
    public void onLoad() {
        bootstrap = new Bootstrap(this);
        bootstrap.onLoad();
    }

    @Override
    public void onEnable() {
        bootstrap.onEnable();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.onDisable();
        }
    }

    /**
     * Reloads the cleanup tasks, refreshing any scheduled cycles and configuration values.
     */
    public void reloadPluginConfiguration() {
        if (bootstrap != null) {
            bootstrap.reloadConfiguration();
        }
    }

    /**
     * Gets the economy provider if available.
     * @deprecated Use Registry.getEconomy() instead
     */
    @Deprecated
    public @Nullable Economy getEconomy() {
        return Registry.getEconomy();
    }

    /**
     * Gets the cleanup statistics tracker.
     * @deprecated Use Registry.getStatsTracker() instead
     */
    @Deprecated
    public com.skyblockexp.ezclean.stats.CleanupStatsTracker getStatsTracker() {
        return Registry.getStatsTracker();
    }

    @Override
    public FileConfiguration getConfig() {
        return configuration;
    }
}
