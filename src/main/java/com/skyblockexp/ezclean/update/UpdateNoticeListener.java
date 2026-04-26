package com.skyblockexp.ezclean.update;

import com.skyblockexp.ezclean.command.Msg;
import com.skyblockexp.ezclean.util.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Notifies operators in-game when a newer version of EzClean is available.
 *
 * <p>The notice is sent shortly after join so it doesn't get buried under
 * the join flood of messages.
 */
public final class UpdateNoticeListener implements Listener {

    private final SpigotUpdateChecker updateChecker;

    public UpdateNoticeListener(SpigotUpdateChecker updateChecker) {
        this.updateChecker = updateChecker;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("ezclean.reload")) {
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(UpdateNoticeListener.class);
        // Delay 2 s (40 ticks) so the notice appears after the join spam settles.
        FoliaScheduler.runGlobalLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            String version = updateChecker.getAvailableVersion();
            if (version == null || version.isEmpty()) {
                return;
            }
            Component notice = Msg.warn(Msg.t("update.available", "version", version))
                    .append(Component.text(Msg.t("update.download"),
                            NamedTextColor.AQUA,
                            TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(updateChecker.getResourceUrl()))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                    Component.text(updateChecker.getResourceUrl(),
                                            NamedTextColor.GRAY))));
            player.sendMessage(notice);
        }, 40L);
    }
}
