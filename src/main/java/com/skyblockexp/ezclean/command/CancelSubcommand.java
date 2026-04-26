package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.config.CleanupCancelSettings;
import com.skyblockexp.ezclean.config.CleanupSettings;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import com.skyblockexp.ezclean.Registry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the "cancel" subcommand for canceling upcoming cleanups.
 */
public class CancelSubcommand implements Subcommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final EntityCleanupScheduler cleanupScheduler;

    public CancelSubcommand(EzCleanPlugin plugin, EntityCleanupScheduler cleanupScheduler) {
        this.cleanupScheduler = cleanupScheduler;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Msg.error(Msg.t("command.cancel.players-only")));
            return true;
        }

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
                        .append(net.kyori.adventure.text.Component.text(Msg.t("command.cancel.multiple"), net.kyori.adventure.text.format.NamedTextColor.RED))
                        .append(net.kyori.adventure.text.Component.text(String.join(", ", cleanerIds), net.kyori.adventure.text.format.NamedTextColor.AQUA)));
            } else if (args.length > 0) {
                sender.sendMessage(Msg.PREFIX
                        .append(net.kyori.adventure.text.Component.text(Msg.t("command.cancel.no-match", "id", args[0]), net.kyori.adventure.text.format.NamedTextColor.RED))
                        .append(net.kyori.adventure.text.Component.text(Msg.t("command.cancel.available-label"), net.kyori.adventure.text.format.NamedTextColor.GRAY))
                        .append(net.kyori.adventure.text.Component.text(String.join(", ", cleanerIds), net.kyori.adventure.text.format.NamedTextColor.AQUA)));
            }
            return true;
        }

        CleanupSettings settings = cleanupScheduler.getSettings(cleanerId);
        if (settings == null) {
            sender.sendMessage(Msg.error(Msg.t("command.cancel.no-match-simple", "id", cleanerId)));
            return true;
        }

        CleanupCancelSettings cancelSettings = settings.getCancelSettings();
        if (!cancelSettings.isEnabled()) {
            sendMiniMessage(player, cancelSettings.getDisabledMessage(),
                    Placeholder.parsed("cleaner", settings.getCleanerId()));
            return true;
        }

        Duration remaining = cleanupScheduler.getTimeUntilCleanup(cleanerId);
        if (remaining == null) {
            sender.sendMessage(Msg.error(Msg.t("command.cancel.idle", "id", cleanerId)));
            return true;
        }

        Economy economy = Registry.getEconomy();
        if (economy == null) {
            sendMiniMessage(player, cancelSettings.getNoEconomyMessage(),
                    Placeholder.parsed("cleaner", settings.getCleanerId()));
            return true;
        }

        double cost = cancelSettings.getCost();
        if (cost > 0.0D) {
            double balance = economy.getBalance(player);
            if (balance + 1.0E-9 < cost) {
                sendMiniMessage(player, cancelSettings.getInsufficientFundsMessage(),
                        Placeholder.parsed("cleaner", settings.getCleanerId()),
                        Placeholder.parsed("cost", cancelSettings.getFormattedCost()));
                return true;
            }
            EconomyResponse withdrawal = economy.withdrawPlayer(player, cost);
            if (withdrawal == null || withdrawal.type != EconomyResponse.ResponseType.SUCCESS) {
                String error = withdrawal != null ? withdrawal.errorMessage : "Economy error.";
                sender.sendMessage(Msg.error(Msg.t("command.cancel.funding-error", "error", error)));
                return true;
            }
        }

        Duration delay = cleanupScheduler.cancelNextCleanup(cleanerId);
        if (delay == null) {
            if (cost > 0.0D) {
                economy.depositPlayer(player, cost);
            }
            sender.sendMessage(Msg.error(Msg.t("command.cancel.cancel-failed")));
            return true;
        }

        long minutes = Math.max(1L, delay.toMinutes());
        TagResolver[] placeholders = new TagResolver[] {
                Placeholder.parsed("player", player.getName()),
                Placeholder.parsed("cleaner", settings.getCleanerId()),
                Placeholder.parsed("cost", cancelSettings.getFormattedCost()),
                Placeholder.parsed("minutes", Long.toString(minutes))
        };

        sendMiniMessage(player, cancelSettings.getSuccessMessage(), placeholders);

        String broadcastTemplate = cancelSettings.getBroadcastMessage();
        if (broadcastTemplate != null && !broadcastTemplate.isEmpty()) {
            Component component = MINI_MESSAGE.deserialize(broadcastTemplate, placeholders);
            Bukkit.getServer().sendMessage(component);
        }
        return true;
    }

    @Override
    public String getName() {
        return "cancel";
    }

    @Override
    public String getPermission() {
        return "ezclean.cancel";
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

    private void sendMiniMessage(Player player, String template, TagResolver... placeholders) {
        if (template == null || template.isEmpty()) {
            return;
        }
        Component component = MINI_MESSAGE.deserialize(template, placeholders);
        player.sendMessage(component);
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Msg.error("Usage: /" + label + " cancel [cleaner_id]"));
    }
}
