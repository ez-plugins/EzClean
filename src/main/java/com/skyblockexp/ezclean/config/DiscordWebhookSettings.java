package com.skyblockexp.ezclean.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable configuration for the Discord webhook integration.
 *
 * <p>When enabled, EzClean posts a summary embed to a Discord channel after every
 * cleanup cycle. The embed includes the cleaner ID, entity counts by removal group,
 * server TPS before/after, and elapsed duration.</p>
 *
 * <p>Loaded from the global {@code config.yml} under the {@code discord} key.
 * Returns {@code null} from {@link #load} when disabled or misconfigured so callers
 * can treat absence as a no-op without null-safe branches everywhere.</p>
 */
public final class DiscordWebhookSettings {

    private final String webhookUrl;
    private final boolean sendCleanupSummary;
    private final int embedColor;

    private DiscordWebhookSettings(String webhookUrl, boolean sendCleanupSummary, int embedColor) {
        this.webhookUrl = webhookUrl;
        this.sendCleanupSummary = sendCleanupSummary;
        this.embedColor = embedColor;
    }

    /**
     * Loads Discord webhook settings from the {@code discord} section of {@code config.yml}.
     *
     * @param globalConfig the plugin's root configuration section
     * @return a configured instance, or {@code null} when disabled or webhook URL is blank
     */
    public static @Nullable DiscordWebhookSettings load(@Nullable ConfigurationSection globalConfig) {
        if (globalConfig == null) {
            return null;
        }
        ConfigurationSection discord = globalConfig.getConfigurationSection("discord");
        if (discord == null) {
            return null;
        }
        if (!discord.getBoolean("enabled", false)) {
            return null;
        }

        String url = discord.getString("webhook-url", "").trim();
        if (url.isEmpty()) {
            return null;
        }

        // Validate that the URL looks like a Discord webhook to prevent SSRF misuse.
        if (!url.startsWith("https://discord.com/api/webhooks/")
                && !url.startsWith("https://discordapp.com/api/webhooks/")
                && !url.startsWith("https://ptb.discord.com/api/webhooks/")
                && !url.startsWith("https://canary.discord.com/api/webhooks/")) {
            return null;
        }

        boolean sendSummary = discord.getBoolean("events.cleanup-summary", true);
        String colorHex = discord.getString("embed-color", "5865F2").trim().replace("#", "");
        int embedColor;
        try {
            embedColor = Integer.parseUnsignedInt(colorHex, 16);
        } catch (NumberFormatException ignored) {
            embedColor = 0x5865F2; // Discord blurple
        }

        return new DiscordWebhookSettings(url, sendSummary, embedColor);
    }

    /** The Discord webhook URL to POST to. Always a fully-qualified {@code https://discord.com} URL. */
    public String getWebhookUrl() {
        return webhookUrl;
    }

    /** Whether to send a summary embed after each cleanup cycle completes. */
    public boolean isSendCleanupSummary() {
        return sendCleanupSummary;
    }

    /** Decimal color value used for the Discord embed left-side stripe. */
    public int getEmbedColor() {
        return embedColor;
    }
}
