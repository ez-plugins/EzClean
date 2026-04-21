package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides command handling for managing EzClean cleaners, including manual runs and reloads.
 */
public final class EzCleanCommand implements CommandExecutor, TabCompleter {

    private final Map<String, Subcommand> subcommands = new HashMap<>();

    public EzCleanCommand(EzCleanPlugin plugin, EntityCleanupScheduler cleanupScheduler) {
        // Register subcommands
        subcommands.put("run", new RunSubcommand(cleanupScheduler));
        subcommands.put("cancel", new CancelSubcommand(plugin, cleanupScheduler));
        subcommands.put("reload", new ReloadSubcommand(plugin));
        subcommands.put("time", new TimeSubcommand(cleanupScheduler));
        subcommands.put("usage", new UsageSubcommand(plugin));
        subcommands.put("stats", new StatsSubcommand(plugin, cleanupScheduler));
        subcommands.put("toggle", new ToggleSubcommand(plugin, cleanupScheduler));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String subcommandName = args[0].toLowerCase(Locale.ROOT);
        Subcommand subcommand = subcommands.get(subcommandName);

        if (subcommand == null) {
            sendUsage(sender, label);
            return true;
        }

        // Check permission
        String permission = subcommand.getPermission();
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Execute subcommand with remaining args
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subcommand.execute(sender, label, subArgs);
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return "less than a minute";
        }

        long totalSeconds = duration.getSeconds();
        if (totalSeconds <= 0L) {
            return "less than a minute";
        }

        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            if (minutes > 0L) {
                return hours + " hour" + (hours == 1L ? "" : "s") + " and " + minutes + " minute"
                        + (minutes == 1L ? "" : "s");
            }
            if (seconds > 0L) {
                return hours + " hour" + (hours == 1L ? "" : "s") + " and " + seconds + " second"
                        + (seconds == 1L ? "" : "s");
            }
            return hours + " hour" + (hours == 1L ? "" : "s");
        }

        if (minutes > 0L) {
            if (seconds > 0L) {
                return minutes + " minute" + (minutes == 1L ? "" : "s") + " and " + seconds + " second"
                        + (seconds == 1L ? "" : "s");
            }
            return minutes + " minute" + (minutes == 1L ? "" : "s");
        }

        return seconds + " second" + (seconds == 1L ? "" : "s");
    }

    private List<String> formatStatsSnapshot(CleanupStatsTracker.CleanupStatsSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        CleanupStatsTracker.CleanupStatsSnapshot.Totals totals = snapshot.totals();
        CleanupStatsTracker.CleanupStatsSnapshot.LastRun lastRun = snapshot.lastRun();

        lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Cleanup stats for " + snapshot.cleanerId());
        lines.add(ChatColor.GRAY + "Total runs: " + ChatColor.AQUA + totals.runs());
        lines.add(ChatColor.GRAY + "Total removed: " + ChatColor.AQUA + totals.removed());
        lines.add(ChatColor.GRAY + "Average duration: " + ChatColor.AQUA + formatDuration(totals.averageDurationMillis()));
        lines.add(ChatColor.GRAY + "Average TPS impact: " + ChatColor.AQUA + formatTpsImpact(totals.averageTpsImpact()));
        lines.add(ChatColor.GRAY + "Top entity groups: " + ChatColor.AQUA
                + formatTopEntries(totals.groupTotals(), 5));
        lines.add(ChatColor.GRAY + "Top worlds: " + ChatColor.AQUA
                + formatTopEntries(totals.worldTotals(), 5));

        if (lastRun != null) {
            lines.add(ChatColor.DARK_GRAY + "Last run:");
            lines.add(ChatColor.GRAY + " - Removed: " + ChatColor.AQUA + lastRun.removed());
            lines.add(ChatColor.GRAY + " - Duration: " + ChatColor.AQUA + formatDuration(lastRun.durationMillis()));
            lines.add(ChatColor.GRAY + " - TPS impact: " + ChatColor.AQUA + formatTpsImpact(lastRun.tpsImpact()));
            lines.add(ChatColor.GRAY + " - Groups: " + ChatColor.AQUA
                    + formatTopEntries(lastRun.groupCounts(), 5));
            lines.add(ChatColor.GRAY + " - Worlds: " + ChatColor.AQUA
                    + formatTopEntries(lastRun.worldCounts(), 5));
        }

        return lines;
    }

    private String formatTopEntries(Map<String, ? extends Number> entries, int limit) {
        if (entries.isEmpty()) {
            return "None";
        }
        List<Map.Entry<String, ? extends Number>> sorted = new ArrayList<>(entries.entrySet());
        sorted.sort((left, right) -> {
            int countCompare = Double.compare(right.getValue().doubleValue(), left.getValue().doubleValue());
            if (countCompare != 0) {
                return countCompare;
            }
            return left.getKey().compareToIgnoreCase(right.getKey());
        });
        StringBuilder builder = new StringBuilder();
        int max = Math.min(limit, sorted.size());
        for (int i = 0; i < max; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            Map.Entry<String, ? extends Number> entry = sorted.get(i);
            builder.append(entry.getKey()).append(" (").append(entry.getValue()).append(")");
        }
        return builder.toString();
    }

    private String formatDuration(@Nullable Long durationMillis) {
        if (durationMillis == null) {
            return "N/A";
        }
        if (durationMillis < 1000L) {
            return durationMillis + "ms";
        }
        return String.format(Locale.ROOT, "%.2fs", durationMillis / 1000.0);
    }

    private String formatTpsImpact(@Nullable Double tpsImpact) {
        if (tpsImpact == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.2f", tpsImpact);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (Subcommand subcommand : subcommands.values()) {
                String permission = subcommand.getPermission();
                if (permission == null || sender.hasPermission(permission)) {
                    if (subcommand.getName().startsWith(prefix)) {
                        suggestions.add(subcommand.getName());
                    }
                }
            }
            return suggestions;
        }

        if (args.length >= 2) {
            String subcommandName = args[0].toLowerCase(Locale.ROOT);
            Subcommand subcommand = subcommands.get(subcommandName);
            if (subcommand != null) {
                String permission = subcommand.getPermission();
                if (permission == null || sender.hasPermission(permission)) {
                    String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                    return subcommand.tabComplete(sender, subArgs);
                }
            }
        }

        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " run [cleaner_id]");
        sender.sendMessage(ChatColor.RED + "       /" + label + " cancel [cleaner_id]");
        sender.sendMessage(ChatColor.RED + "       /" + label + " reload");
        sender.sendMessage(ChatColor.RED + "       /" + label + " time [cleaner_id]");
        sender.sendMessage(ChatColor.RED + "       /" + label + " usage [plugin|live|stop] [plugin]");
        sender.sendMessage(ChatColor.RED + "       /" + label + " stats [cleaner_id]");
    }
}
