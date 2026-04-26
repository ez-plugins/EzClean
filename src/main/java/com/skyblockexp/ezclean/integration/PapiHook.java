package com.skyblockexp.ezclean.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Thin wrapper around PlaceholderAPI for processing placeholder strings in
 * EzClean messages. All methods are safe to call even when PlaceholderAPI is
 * not installed — they return the original text unchanged in that case.
 *
 * <p>Use {@link #setPlaceholders(Player, String)} when a specific player context
 * is available (e.g. cancel confirmation messages). Use
 * {@link #setPlaceholders(String)} for server-scoped broadcast templates where no
 * individual player context applies (e.g. countdown and summary broadcasts);
 * placeholders that require a player will be removed/left empty in that case.</p>
 */
public final class PapiHook {

    private PapiHook() {}

    /**
     * Returns {@code true} when PlaceholderAPI is present and enabled.
     */
    public static boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    /**
     * Processes PlaceholderAPI placeholders in {@code text} using the given
     * online {@code player} as context. Returns {@code text} unchanged when
     * PlaceholderAPI is not installed.
     *
     * @param player the player to resolve player-specific placeholders for
     * @param text   the text containing {@code %placeholder%} tokens
     * @return the processed text
     */
    public static String setPlaceholders(Player player, String text) {
        if (!isEnabled() || text == null || text.isEmpty()) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    /**
     * Processes PlaceholderAPI placeholders in {@code text} without a specific
     * player context. Player-dependent placeholders will resolve to their
     * default/empty value. Returns {@code text} unchanged when PlaceholderAPI
     * is not installed.
     *
     * @param text the text containing {@code %placeholder%} tokens
     * @return the processed text
     */
    public static String setPlaceholders(String text) {
        if (!isEnabled() || text == null || text.isEmpty()) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(null, text);
    }

    /**
     * Processes PlaceholderAPI placeholders with an optional {@link OfflinePlayer}
     * context. Delegates to the player-aware or no-player overload as appropriate.
     *
     * @param player nullable offline-player context; {@code null} for server-global processing
     * @param text   the text containing {@code %placeholder%} tokens
     * @return the processed text
     */
    public static String setPlaceholders(@Nullable OfflinePlayer player, String text) {
        if (!isEnabled() || text == null || text.isEmpty()) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
