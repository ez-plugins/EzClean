package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the "run" subcommand for manually triggering cleanups.
 */
public class RunSubcommand implements Subcommand {

    private final EntityCleanupScheduler cleanupScheduler;

    public RunSubcommand(EntityCleanupScheduler cleanupScheduler) {
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
                sender.sendMessage(Msg.error(Msg.t("command.run.multiple", "ids", String.join(", ", cleanerIds))));
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
                sender.sendMessage(Msg.PREFIX
                        .append(Component.text(Msg.t("command.run.no-match", "id", requestedId), NamedTextColor.RED))
                        .append(Component.text(Msg.t("command.run.available"), NamedTextColor.GRAY))
                        .append(Component.text(String.join(", ", cleanerIds), NamedTextColor.AQUA)));
                return true;
            }
        }

        if (!cleanupScheduler.triggerCleanup(cleanerId)) {
            sender.sendMessage(Msg.error(Msg.t("command.run.idle", "id", cleanerId)));
            return true;
        }

        sender.sendMessage(Msg.success(Msg.t("command.run.triggered", "id", cleanerId)));
        return true;
    }

    @Override
    public String getName() {
        return "run";
    }

    @Override
    public String getPermission() {
        return "ezclean.clean";
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
        sender.sendMessage(Msg.error("Usage: /" + label + " run [cleaner_id]"));
    }
}
