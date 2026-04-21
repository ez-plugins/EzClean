package com.skyblockexp.ezclean.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageProvider {
    private final ConfigurationSection messagesConfig;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public MessageProvider(ConfigurationSection messagesConfig) {
        this.messagesConfig = messagesConfig;
    }

    public static MessageProvider from(ConfigurationSection section) {
        return new MessageProvider(section);
    }

    public String getRaw(String path, Map<String, String> placeholders) {
        String template = messagesConfig.getString(path);
        if (template == null) return "";
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', template);
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String msg = getRaw(path, placeholders);
        if (msg.isEmpty()) return;
        sender.sendMessage(msg);
    }

    public void sendMiniMessage(Player player, String path, TagResolver... resolvers) {
        String template = messagesConfig.getString(path);
        if (template == null || template.isEmpty()) return;
        player.sendMessage(MINI_MESSAGE.deserialize(template, resolvers));
    }

    public void broadcast(String path, TagResolver... resolvers) {
        String template = messagesConfig.getString(path);
        if (template == null || template.isEmpty()) return;
        Component message = MINI_MESSAGE.deserialize(template, resolvers);
        String legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(message);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(legacy));
        Bukkit.getServer().getConsoleSender().sendMessage(legacy);
    }

    public void broadcastRawMessage(String message) {
        if (message == null || message.isEmpty()) return;
        String legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(MINI_MESSAGE.deserialize(message));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(legacy));
        Bukkit.getServer().getConsoleSender().sendMessage(legacy);
    }
}
