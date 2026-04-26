package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the "time" subcommand for checking time until next cleanup.
 */
public class TimeSubcommand implements Subcommand {

    private final EntityCleanupScheduler cleanupScheduler;

    public TimeSubcommand(EntityCleanupScheduler cleanupScheduler) {
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

        String cleanerId = null;
        if (args.length == 0) {
            if (cleanerIds.size() == 1) {
                cleanerId = cleanerIds.get(0);
            } else {
                // Show all profiles as a clickable list
                sender.sendMessage(Msg.PREFIX
                        .append(Component.text(Msg.t("command.time.header"), NamedTextColor.GOLD, TextDecoration.BOLD)));
                for (String id : cleanerIds) {
                    Duration remaining = cleanupScheduler.getTimeUntilCleanup(id);
                    Component timeText = remaining == null
                            ? Component.text(Msg.t("command.time.not-scheduled"), NamedTextColor.RED)
                            : Component.text(Msg.formatDuration(remaining), NamedTextColor.GREEN);

                    String cancelCmd = "/" + label + " cancel " + id;
                    Component row = Component.text("  " + id, NamedTextColor.AQUA)
                            .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                            .append(timeText);

                    if (remaining != null) {
                        row = row.hoverEvent(HoverEvent.showText(
                                        Component.text(Msg.t("command.time.hover-cancel", "id", id), NamedTextColor.YELLOW)))
                                .clickEvent(ClickEvent.suggestCommand(cancelCmd));
                    }
                    sender.sendMessage(row);
                }
                return true;
            }
        } else {
            String requestedId = args[0];
            for (String candidate : cleanerIds) {
                if (candidate.equalsIgnoreCase(requestedId)) {
                    cleanerId = candidate;
                    break;
                }
            }
            if (cleanerId == null) {
                sender.sendMessage(Msg.error(Msg.t("command.time.no-match", "id", requestedId)));
                return true;
            }
        }

        Duration timeRemaining = cleanupScheduler.getTimeUntilCleanup(cleanerId);
        if (timeRemaining == null) {
            sender.sendMessage(Msg.warn(Msg.t("command.time.idle", "id", cleanerId)));
            return true;
        }

        sender.sendMessage(Msg.success(Msg.t("command.time.result", "id", cleanerId, "duration", Msg.formatDuration(timeRemaining))));
        return true;
    }

    @Override
    public String getName() {
        return "time";
    }

    @Override
    public String getPermission() {
        return "ezclean.status";
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

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Msg.error("Usage: /" + label + " time [cleaner_id]"));
    }
}
