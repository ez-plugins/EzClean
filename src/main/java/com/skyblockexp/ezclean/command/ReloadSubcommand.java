package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import java.util.List;

/**
 * Handles the "reload" subcommand for reloading plugin configuration.
 */
public class ReloadSubcommand implements Subcommand {

    private final EzCleanPlugin plugin;

    public ReloadSubcommand(EzCleanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        plugin.reloadPluginConfiguration();
        sender.sendMessage(ChatColor.GREEN + "Reloaded EzClean configuration.");
        return true;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "ezclean.reload";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
