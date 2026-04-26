package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

        sender.sendMessage(Msg.error(Msg.t("command.toggle.unknown-feature", "name", args[0])));
        sender.sendMessage(Component.text(Msg.t("command.toggle.cleaner-features"), NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", CLEANER_FEATURES), NamedTextColor.AQUA)));
        sender.sendMessage(Component.text(Msg.t("command.toggle.global-features"), NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", GLOBAL_FEATURES), NamedTextColor.AQUA)));
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
                sender.sendMessage(Msg.PREFIX
                        .append(Component.text(Msg.t("command.toggle.no-match", "id", args[1]), NamedTextColor.RED))
                        .append(Component.text(Msg.t("command.toggle.available"), NamedTextColor.GRAY))
                        .append(Component.text(String.join(", ", ids), NamedTextColor.AQUA)));
                return;
            }
        } else if (ids.size() == 1) {
            cleanerId = ids.get(0);
        } else {
            sender.sendMessage(Msg.PREFIX
                    .append(Component.text(Msg.t("command.toggle.multiple"), NamedTextColor.RED))
                    .append(Component.text(String.join(", ", ids), NamedTextColor.AQUA)));
            return;
        }

        File cleanerFile = new File(plugin.getDataFolder(), "cleaners/" + cleanerId + ".yml");
        if (!cleanerFile.exists()) {
            sender.sendMessage(Msg.error(Msg.t("command.toggle.no-file", "id", cleanerId)));
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(cleanerFile);
        String configKey = featureToConfigKey(feature);
        boolean oldValue = config.getBoolean(configKey, false);
        boolean newValue = !oldValue;
        config.set(configKey, newValue);

        try {
            config.save(cleanerFile);
        } catch (IOException ex) {
            sender.sendMessage(Msg.error(Msg.t("command.toggle.save-failed", "id", cleanerId, "error", ex.getMessage())));
            plugin.getLogger().warning("Failed to save cleaner config '" + cleanerFile.getName()
                    + "': " + ex.getMessage());
            return;
        }

        plugin.reloadPluginConfiguration();
        sender.sendMessage(Msg.PREFIX
                .append(Component.text(feature, NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" [", NamedTextColor.DARK_GRAY))
                .append(Component.text(cleanerId, NamedTextColor.AQUA))
                .append(Component.text("]: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(oldValue ? Msg.t("command.toggle.on") : Msg.t("command.toggle.off"), oldValue ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                .append(Component.text(newValue ? Msg.t("command.toggle.on") : Msg.t("command.toggle.off"), newValue ? NamedTextColor.GREEN : NamedTextColor.RED)));
    }

    private void toggleGlobal(CommandSender sender, String feature) {
        File globalFile = resolveGlobalFile(feature);
        if (globalFile == null) {
            sender.sendMessage(Msg.error(Msg.t("command.toggle.no-global-file", "name", feature)));
            return;
        }
        if (!globalFile.exists()) {
            sender.sendMessage(Msg.error(Msg.t("command.toggle.no-global-file-missing", "name", feature)));
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(globalFile);
        String configKey = featureToConfigKey(feature);
        boolean oldValue = config.getBoolean(configKey, false);
        boolean newValue = !oldValue;
        config.set(configKey, newValue);

        try {
            config.save(globalFile);
        } catch (IOException ex) {
            sender.sendMessage(Msg.error(Msg.t("command.toggle.save-failed-global", "error", ex.getMessage())));
            plugin.getLogger().warning("Failed to save global config '" + globalFile.getName()
                    + "': " + ex.getMessage());
            return;
        }

        plugin.reloadPluginConfiguration();
        sender.sendMessage(Msg.PREFIX
                .append(Component.text(feature, NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(oldValue ? Msg.t("command.toggle.on") : Msg.t("command.toggle.off"), oldValue ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                .append(Component.text(newValue ? Msg.t("command.toggle.on") : Msg.t("command.toggle.off"), newValue ? NamedTextColor.GREEN : NamedTextColor.RED)));
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
        sender.sendMessage(Msg.warn("Usage: /" + label + " toggle <feature> [cleaner_id]"));
        sender.sendMessage(Component.text(Msg.t("command.toggle.cleaner-features"), NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", CLEANER_FEATURES), NamedTextColor.AQUA)));
        sender.sendMessage(Component.text(Msg.t("command.toggle.global-features"), NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", GLOBAL_FEATURES), NamedTextColor.AQUA)));
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
