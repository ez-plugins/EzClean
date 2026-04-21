package com.skyblockexp.ezclean.command;

import org.bukkit.command.CommandSender;
import java.util.List;

/**
 * Interface for EzClean subcommands.
 */
public interface Subcommand {

    /**
     * Executes the subcommand.
     *
     * @param sender the command sender
     * @param label the command label
     * @param args the command arguments (excluding the subcommand name)
     * @return true if the command was handled successfully
     */
    boolean execute(CommandSender sender, String label, String[] args);

    /**
     * Gets the name of the subcommand.
     *
     * @return the subcommand name
     */
    String getName();

    /**
     * Gets the permission required for this subcommand.
     *
     * @return the permission string, or null if no permission required
     */
    String getPermission();

    /**
     * Provides tab completion for this subcommand.
     *
     * @param sender the command sender
     * @param args the command arguments (excluding the subcommand name)
     * @return list of tab completion suggestions
     */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
