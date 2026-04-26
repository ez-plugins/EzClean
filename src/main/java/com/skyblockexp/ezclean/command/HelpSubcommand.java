package com.skyblockexp.ezclean.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import java.util.List;

/**
 * Handles the "help" subcommand for displaying command usage information.
 */
public class HelpSubcommand implements Subcommand {

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        sender.sendMessage(Msg.separator());
        sender.sendMessage(Msg.PREFIX
                .append(Component.text("EzClean", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" — " + Msg.t("command.help.title"), NamedTextColor.GRAY)));
        sender.sendMessage(Msg.separator());

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  " + Msg.t("command.help.section-player"), NamedTextColor.YELLOW, TextDecoration.BOLD));
        sendEntry(sender, label, "time [id]",   Msg.t("command.help.cmd-time"),   "ezclean.status");
        sendEntry(sender, label, "cancel [id]", Msg.t("command.help.cmd-cancel"), "ezclean.cancel");
        sendEntry(sender, label, "stats [id]",  Msg.t("command.help.cmd-stats"),  "ezclean.stats");
        sendEntry(sender, label, "help",         Msg.t("command.help.cmd-help"),   "ezclean.help");

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  " + Msg.t("command.help.section-admin"), NamedTextColor.YELLOW, TextDecoration.BOLD));
        sendEntry(sender, label, "setup",                    Msg.t("command.help.cmd-setup"),  "ezclean.setup");
        sendEntry(sender, label, "run [id]",             Msg.t("command.help.cmd-run"),    "ezclean.clean");
        sendEntry(sender, label, "reload",                Msg.t("command.help.cmd-reload"), "ezclean.reload");
        sendEntry(sender, label, "toggle <feature> [id]", Msg.t("command.help.cmd-toggle"), "ezclean.toggle");
        sendEntry(sender, label, "usage [plugin|live|stop]", Msg.t("command.help.cmd-usage"), "ezclean.usage");

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  " + Msg.t("command.help.id-note"),
                NamedTextColor.DARK_GRAY));
        sender.sendMessage(Msg.separator());
        return true;
    }

    private void sendEntry(CommandSender sender, String label, String sub, String desc, String perm) {
        // Only show the first token as the clickable suggest command
        String baseCmd = "/" + label + " " + sub.split(" ")[0] + " ";
        Component command = Component.text("  /" + label + " " + sub, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.suggestCommand(baseCmd))
                .hoverEvent(HoverEvent.showText(
                        Component.text(Msg.t("command.help.hover-fill") + "\n", NamedTextColor.YELLOW)
                                .append(Component.text(perm, NamedTextColor.DARK_GRAY))));
        sender.sendMessage(command
                .append(Component.text(" — " + desc, NamedTextColor.GRAY))
                .append(Component.text("  " + perm, NamedTextColor.DARK_GRAY)));
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getPermission() {
        return "ezclean.help";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
