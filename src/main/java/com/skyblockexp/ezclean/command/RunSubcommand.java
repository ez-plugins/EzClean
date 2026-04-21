package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "No cleanup profiles are currently configured.");
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
                sender.sendMessage(ChatColor.RED + "Multiple cleanup profiles available. Specify one of: "
                        + String.join(", ", cleanerIds));
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
                sender.sendMessage(ChatColor.RED + "No cleanup profile matches \"" + requestedId + "\".");
                return true;
            }
        }

        if (!cleanupScheduler.triggerCleanup(cleanerId)) {
            sender.sendMessage(ChatColor.RED + "That cleanup profile is not currently scheduled.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Triggered cleanup for profile \"" + cleanerId + "\".");
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
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " run [cleaner_id]");
    }
}
