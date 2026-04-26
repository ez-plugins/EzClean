package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.Registry;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
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
            sender.sendMessage(Msg.error(Msg.t("command.no-profiles")));
            return true;
        }

        if (args.length > 1) {
            sendUsage(sender, label);
            return true;
        }

        String cleanerId = resolveCleanerId(args, cleanerIds);
        if (cleanerId == null) {
            if (args.length == 0 && cleanerIds.size() > 1) {
                sender.sendMessage(Msg.PREFIX
                        .append(Component.text(Msg.t("command.stats.multiple"), NamedTextColor.RED))
                        .append(Component.text(String.join(", ", cleanerIds), NamedTextColor.AQUA)));
            } else if (args.length > 0) {
                sender.sendMessage(Msg.error(Msg.t("command.stats.no-match", "id", args[0])));
            }
            return true;
        }

        CleanupStatsTracker.CleanupStatsSnapshot snapshot = Registry.getStatsTracker().getSnapshot(cleanerId);
        if (snapshot == null || snapshot.totals().runs() == 0L) {
            sender.sendMessage(Msg.warn(Msg.t("command.stats.no-stats", "id", cleanerId)));
            return true;
        }

        sendSnapshot(sender, snapshot);
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

    private void sendSnapshot(CommandSender sender, CleanupStatsTracker.CleanupStatsSnapshot snapshot) {
        CleanupStatsTracker.CleanupStatsSnapshot.Totals totals = snapshot.totals();
        CleanupStatsTracker.CleanupStatsSnapshot.LastRun lastRun = snapshot.lastRun();

        sender.sendMessage(Msg.PREFIX
                .append(Component.text(Msg.t("command.stats.header"), NamedTextColor.GRAY))
                .append(Component.text(snapshot.cleanerId(), NamedTextColor.AQUA, TextDecoration.BOLD)));

        sender.sendMessage(statRow(Msg.t("command.stats.row-total-runs"),    String.valueOf(totals.runs())));
        sender.sendMessage(statRow(Msg.t("command.stats.row-total-removed"), String.valueOf(totals.removed())));
        sender.sendMessage(statRow(Msg.t("command.stats.row-avg-duration"),  Msg.formatMs(totals.averageDurationMillis())));
        sender.sendMessage(statRow(Msg.t("command.stats.row-avg-tps"),       Msg.formatTpsImpact(totals.averageTpsImpact())));

        sender.sendMessage(Component.text(Msg.t("command.stats.top-groups"), NamedTextColor.GRAY));
        for (Component c : Msg.formatTopEntriesAsComponents(totals.groupTotals(), 5, null)) {
            sender.sendMessage(c);
        }
        sender.sendMessage(Component.text(Msg.t("command.stats.top-worlds"), NamedTextColor.GRAY));
        for (Component c : Msg.formatTopEntriesAsComponents(totals.worldTotals(), 5, null)) {
            sender.sendMessage(c);
        }

        if (lastRun != null) {
            sender.sendMessage(Msg.separator());
            sender.sendMessage(Component.text("  " + Msg.t("command.stats.last-run"), NamedTextColor.YELLOW, TextDecoration.BOLD));
            sender.sendMessage(statRow(Msg.t("command.stats.last-removed"),  String.valueOf(lastRun.removed())));
            sender.sendMessage(statRow(Msg.t("command.stats.last-duration"), Msg.formatMs(lastRun.durationMillis())));
            sender.sendMessage(statRow(Msg.t("command.stats.last-tps"),      Msg.formatTpsImpact(lastRun.tpsImpact())));
            sender.sendMessage(Component.text(Msg.t("command.stats.last-groups"), NamedTextColor.GRAY));
            for (Component c : Msg.formatTopEntriesAsComponents(lastRun.groupCounts(), 5, null)) {
                sender.sendMessage(c);
            }
            sender.sendMessage(Component.text(Msg.t("command.stats.last-worlds"), NamedTextColor.GRAY));
            for (Component c : Msg.formatTopEntriesAsComponents(lastRun.worldCounts(), 5, null)) {
                sender.sendMessage(c);
            }
        }
    }

    /** Builds a label → value row with aqua value colour. */
    private static Component statRow(String label, String value) {
        return Component.text("  " + label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.AQUA));
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Msg.error("Usage: /" + label + " stats [cleaner_id]"));
    }
}
