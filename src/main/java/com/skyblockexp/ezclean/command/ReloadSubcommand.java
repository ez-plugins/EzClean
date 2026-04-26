package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import java.util.List;

/**
 * Handles the "reload" subcommand for reloading plugin configuration.
 */
public class ReloadSubcommand implements Subcommand {

    private final EzCleanPlugin plugin;
    private final EntityCleanupScheduler cleanupScheduler;

    public ReloadSubcommand(EzCleanPlugin plugin, EntityCleanupScheduler cleanupScheduler) {
        this.plugin = plugin;
        this.cleanupScheduler = cleanupScheduler;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        plugin.reloadPluginConfiguration();
        List<String> ids = cleanupScheduler.getCleanerIds();
        String profileCount = ids.size() == 1
                ? Msg.t("command.reload.profile-single")
                : Msg.t("command.reload.profiles-plural", "n", String.valueOf(ids.size()));
        String profileList = ids.isEmpty() ? Msg.t("command.reload.none") : String.join(", ", ids);
        sender.sendMessage(Msg.success(Msg.t("command.reload.success"))
                .append(Component.text(Msg.t("command.reload.loaded-prefix"), NamedTextColor.GRAY))
                .append(Component.text(profileCount, NamedTextColor.AQUA))
                .append(Component.text(Msg.t("command.reload.colon") + profileList, NamedTextColor.GRAY)));
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

