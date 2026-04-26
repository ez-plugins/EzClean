package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.gui.SetupGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /ezclean setup} subcommand, which opens the inventory-based
 * setup GUI so admins can configure cleaner profiles without editing YAML manually.
 */
public final class SetupSubcommand implements Subcommand {

    private final SetupGUI gui;

    public SetupSubcommand(SetupGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Msg.error(Msg.t("command.setup.players-only")));
            return true;
        }
        gui.openList(player);
        return true;
    }

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getPermission() {
        return "ezclean.setup";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
