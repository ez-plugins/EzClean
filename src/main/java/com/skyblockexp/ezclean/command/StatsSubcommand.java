package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.Registry;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles the "stats" subcommand for viewing cleanup statistics.
 */
public class StatsSubcommand implements Subcommand {

    private final EntityCleanupScheduler cleanupScheduler;

    public StatsSubcommand(EzCleanPlugin plugin, EntityCleanupScheduler cleanupScheduler) {
        this.cleanupScheduler = cleanupScheduler;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        List<String> cleanerIds = cleanupScheduler.getCleanerIds();
        if (cleanerIds.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No cleanup profiles are currently configured.");
            return true;
        }

        if (args.length > 1) {
            sendUsage(sender, label);
            return true;
        }

        String cleanerId = resolveCleanerId(args, cleanerIds);
        if (cleanerId == null) {
            if (args.length == 0 && cleanerIds.size() > 1) {
                sender.sendMessage(ChatColor.RED + "Multiple cleanup profiles available. Specify one of: "
                        + String.join(", ", cleanerIds));
            } else if (args.length > 0) {
                sender.sendMessage(ChatColor.RED + "No cleanup profile matches \"" + args[0] + "\".");
            }
            return true;
        }

        CleanupStatsTracker.CleanupStatsSnapshot snapshot = Registry.getStatsTracker().getSnapshot(cleanerId);
        if (snapshot == null || snapshot.totals().runs() == 0L) {
            sender.sendMessage(ChatColor.GRAY + "No cleanup stats recorded yet for \"" + cleanerId + "\".");
            return true;
        }

        for (String line : formatStatsSnapshot(snapshot)) {
            sender.sendMessage(line);
        }
        return true;
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getPermission() {
        return "ezclean.stats";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return cleanupScheduler.getCleanerIds().stream()
                    .filter(id -> id.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }

    private String resolveCleanerId(String[] args, List<String> cleanerIds) {
        if (args.length == 0) {
            if (cleanerIds.size() == 1) {
                return cleanerIds.get(0);
            }
            return null;
        }
        String requestedId = args[0];
        for (String candidate : cleanerIds) {
            if (candidate.equalsIgnoreCase(requestedId)) {
                return candidate;
            }
        }
        return null;
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

    private String formatDuration(@org.jetbrains.annotations.Nullable Long durationMillis) {
        if (durationMillis == null) {
            return "N/A";
        }
        if (durationMillis < 1000L) {
            return durationMillis + "ms";
        }
        return String.format(Locale.ROOT, "%.2fs", durationMillis / 1000.0);
    }

    private String formatTpsImpact(@org.jetbrains.annotations.Nullable Double tpsImpact) {
        if (tpsImpact == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.2f", tpsImpact);
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " stats [cleaner_id]");
    }
}
