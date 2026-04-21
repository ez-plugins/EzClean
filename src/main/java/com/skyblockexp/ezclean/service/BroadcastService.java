package com.skyblockexp.ezclean.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BroadcastService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public void broadcast(Component component) {
        String legacy = LEGACY.serialize(component);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(legacy);
        }
        Bukkit.getServer().getConsoleSender().sendMessage(legacy);
    }

    public void broadcastWithCancel(Component baseMessage, Component interactiveMessage, String cancelPermission) {
        // Console always receives the base message
        String baseLegacy = LEGACY.serialize(baseMessage);
        Bukkit.getServer().getConsoleSender().sendMessage(baseLegacy);

        String interactiveLegacy = LEGACY.serialize(interactiveMessage);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(cancelPermission)) {
                player.sendMessage(interactiveLegacy);
            } else {
                player.sendMessage(baseLegacy);
            }
        }
    }
}
