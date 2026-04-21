package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Handles in-game toggling of performance-sensitive and optional EzClean features.
 *
 * <p>Usage: {@code /ezclean toggle <feature> [cleaner_id]}
 *
 * <p>Cleaner-scoped features require a cleaner ID when more than one profile is loaded.
 * Global features (e.g. death-chests) apply server-wide and don't accept a cleaner ID.
 * The toggle writes the new value to the relevant YAML file and triggers a hot-reload so
 * changes take effect immediately without a server restart.
 */
public class ToggleSubcommand implements Subcommand {

    /** Features stored per cleaner profile in {@code cleaners/<id>.yml}. */
    private static final List<String> CLEANER_FEATURES = Arrays.asList(
            "pile-detection",
            "warning",
            "cancel",
            "interval-broadcast",
            "dynamic-broadcast",
            "stats-summary",
            "async-removal"
    );

    /** Features stored in global config files (no cleaner ID required). */
    private static final List<String> GLOBAL_FEATURES = Collections.singletonList("death-chests");

    private final EzCleanPlugin plugin;
    private final EntityCleanupScheduler cleanupScheduler;

    public ToggleSubcommand(EzCleanPlugin plugin, EntityCleanupScheduler cleanupScheduler) {
        this.plugin = plugin;
        this.cleanupScheduler = cleanupScheduler;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String feature = args[0].toLowerCase(Locale.ROOT);

        if (GLOBAL_FEATURES.contains(feature)) {
            toggleGlobal(sender, feature);
            return true;
        }

        if (CLEANER_FEATURES.contains(feature)) {
            toggleCleaner(sender, feature, args);
            return true;
        }

        List<String> all = getAllFeatures();
        sender.sendMessage(ChatColor.RED + "Unknown feature \"" + args[0] + "\". Available: "
                + String.join(", ", all));
        return true;
    }

    private void toggleCleaner(CommandSender sender, String feature, String[] args) {
        List<String> ids = cleanupScheduler.getCleanerIds();
        String cleanerId;

        if (args.length >= 2) {
            String requested = args[1].toLowerCase(Locale.ROOT);
            cleanerId = null;
            for (String candidate : ids) {
                if (candidate.equalsIgnoreCase(requested)) {
                    cleanerId = candidate;
                    break;
                }
            }
            if (cleanerId == null) {
                sender.sendMessage(ChatColor.RED + "No cleaner profile matches \"" + args[1] + "\". "
                        + "Available: " + String.join(", ", ids));
                return;
            }
        } else if (ids.size() == 1) {
            cleanerId = ids.get(0);
        } else {
            sender.sendMessage(ChatColor.RED + "Multiple cleaner profiles are configured. "
                    + "Specify one: " + String.join(", ", ids));
            return;
        }

        File cleanerFile = new File(plugin.getDataFolder(), "cleaners/" + cleanerId + ".yml");
        if (!cleanerFile.exists()) {
            sender.sendMessage(ChatColor.RED + "No cleaner config file found for \"" + cleanerId + "\".");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(cleanerFile);
        String configKey = featureToConfigKey(feature);
        boolean newValue = !config.getBoolean(configKey, false);
        config.set(configKey, newValue);

        try {
            config.save(cleanerFile);
        } catch (IOException ex) {
            sender.sendMessage(ChatColor.RED + "Failed to save config for cleaner \""
                    + cleanerId + "\": " + ex.getMessage());
            plugin.getLogger().warning("Failed to save cleaner config '" + cleanerFile.getName()
                    + "': " + ex.getMessage());
            return;
        }

        plugin.reloadPluginConfiguration();
        sender.sendMessage(ChatColor.GREEN + "Toggled " + ChatColor.AQUA + feature
                + ChatColor.GREEN + " for cleaner " + ChatColor.AQUA + cleanerId
                + ChatColor.GREEN + ": " + ChatColor.YELLOW + (newValue ? "enabled" : "disabled")
                + ChatColor.GREEN + ". Configuration reloaded.");
    }

    private void toggleGlobal(CommandSender sender, String feature) {
        File globalFile = resolveGlobalFile(feature);
        if (globalFile == null) {
            sender.sendMessage(ChatColor.RED + "Cannot resolve config file for feature \"" + feature + "\".");
            return;
        }
        if (!globalFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Configuration file for \"" + feature + "\" not found. "
                    + "Start the server once to generate it.");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(globalFile);
        String configKey = featureToConfigKey(feature);
        boolean newValue = !config.getBoolean(configKey, false);
        config.set(configKey, newValue);

        try {
            config.save(globalFile);
        } catch (IOException ex) {
            sender.sendMessage(ChatColor.RED + "Failed to save config: " + ex.getMessage());
            plugin.getLogger().warning("Failed to save global config '" + globalFile.getName()
                    + "': " + ex.getMessage());
            return;
        }

        plugin.reloadPluginConfiguration();
        sender.sendMessage(ChatColor.GREEN + "Toggled " + ChatColor.AQUA + feature
                + ChatColor.GREEN + ": " + ChatColor.YELLOW + (newValue ? "enabled" : "disabled")
                + ChatColor.GREEN + ". Configuration reloaded.");
    }

    private String featureToConfigKey(String feature) {
        return switch (feature) {
            case "pile-detection"      -> "pile-detection.enabled";
            case "warning"             -> "warning.enabled";
            case "cancel"              -> "cancel.enabled";
            case "interval-broadcast"  -> "broadcast.interval.enabled";
            case "dynamic-broadcast"   -> "broadcast.dynamic.enabled";
            case "stats-summary"       -> "broadcast.stats-summary.enabled";
            case "async-removal"       -> "performance.async-removal";
            case "death-chests"        -> "death-chests.enabled";
            default                    -> feature;
        };
    }

    private File resolveGlobalFile(String feature) {
        if ("death-chests".equals(feature)) {
            return new File(plugin.getDataFolder(), "death-chests.yml");
        }
        return null;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " toggle <feature> [cleaner_id]");
        sender.sendMessage(ChatColor.GRAY + "Cleaner features: " + ChatColor.AQUA
                + String.join(", ", CLEANER_FEATURES));
        sender.sendMessage(ChatColor.GRAY + "Global features:  " + ChatColor.AQUA
                + String.join(", ", GLOBAL_FEATURES));
    }

    private List<String> getAllFeatures() {
        List<String> all = new ArrayList<>(CLEANER_FEATURES);
        all.addAll(GLOBAL_FEATURES);
        return all;
    }

    @Override
    public String getName() {
        return "toggle";
    }

    @Override
    public String getPermission() {
        return "ezclean.toggle";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            String partial = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> completions = new ArrayList<>();
            for (String feature : getAllFeatures()) {
                if (feature.startsWith(partial)) {
                    completions.add(feature);
                }
            }
            return completions;
        }
        if (args.length == 2) {
            String feature = args[0].toLowerCase(Locale.ROOT);
            if (CLEANER_FEATURES.contains(feature)) {
                String partial = args[1].toLowerCase(Locale.ROOT);
                List<String> completions = new ArrayList<>();
                for (String id : cleanupScheduler.getCleanerIds()) {
                    if (id.toLowerCase(Locale.ROOT).startsWith(partial)) {
                        completions.add(id);
                    }
                }
                return completions;
            }
        }
        return Collections.emptyList();
    }
}
