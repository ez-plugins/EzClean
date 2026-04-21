package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import org.bukkit.ChatColor;
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

        Duration timeRemaining = cleanupScheduler.getTimeUntilCleanup(cleanerId);
        if (timeRemaining == null) {
            sender.sendMessage(ChatColor.RED + "That cleanup profile is not currently scheduled.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Cleanup for profile \"" + cleanerId + "\" will run in "
                + formatDuration(timeRemaining) + ".");
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

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " time [cleaner_id]");
    }
}
