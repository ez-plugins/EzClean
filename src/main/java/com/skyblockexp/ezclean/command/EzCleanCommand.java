package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.gui.SetupGUI;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
    private final EzCleanPlugin plugin;

    public EzCleanCommand(EzCleanPlugin plugin, EntityCleanupScheduler cleanupScheduler, SetupGUI setupGUI) {
        this.plugin = plugin;
        // Register subcommands
        subcommands.put("setup",  new SetupSubcommand(setupGUI));
        subcommands.put("run",    new RunSubcommand(cleanupScheduler));
        subcommands.put("cancel", new CancelSubcommand(plugin, cleanupScheduler));
        subcommands.put("reload", new ReloadSubcommand(plugin, cleanupScheduler));
        subcommands.put("time",   new TimeSubcommand(cleanupScheduler));
        subcommands.put("usage",  new UsageSubcommand(plugin));
        subcommands.put("stats",  new StatsSubcommand(plugin, cleanupScheduler));
        subcommands.put("toggle", new ToggleSubcommand(plugin, cleanupScheduler));
        subcommands.put("help",   new HelpSubcommand());
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
            sender.sendMessage(Msg.error(Msg.t("command.unknown-subcommand", "name", subcommandName, "label", label)));
            return true;
        }

        // Check permission
        String permission = subcommand.getPermission();
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(Msg.error(Msg.t("command.no-permission")));
            return true;
        }

        // Execute subcommand with remaining args
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subcommand.execute(sender, label, subArgs);
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
        sender.sendMessage(Msg.PREFIX
                .append(Component.text("EzClean v" + plugin.getDescription().getVersion(),
                        NamedTextColor.GOLD, TextDecoration.BOLD)));
        sender.sendMessage(Msg.info(Msg.t("command.main.subtitle")));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Msg.warn(Msg.t("command.main.quick-commands")));
        sendClickable(sender, label, "help",     Msg.t("command.main.cmd-help"));
        sendClickable(sender, label, "run [id]",  Msg.t("command.main.cmd-run"));
        sendClickable(sender, label, "time [id]", Msg.t("command.main.cmd-time"));
        sendClickable(sender, label, "stats [id]",Msg.t("command.main.cmd-stats"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Msg.info(Msg.t("command.main.footer", "label", label)));
    }

    private void sendClickable(CommandSender sender, String label, String sub, String desc) {
        Component cmd = Component.text("/" + label + " " + sub, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.suggestCommand("/" + label + " " + sub.split(" ")[0] + " "))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text(desc, NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("  ").append(cmd)
                .append(Component.text(" — " + desc, NamedTextColor.GRAY)));
    }
}
