package com.skyblockexp.ezclean.model;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Settings controlling the optional death chest feature.
 */
public final class DeathChestSettings {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    private final boolean enabled;
    private final Duration despawnAfter;
    private final Component inventoryTitle;
    private final String legacyInventoryTitle;
    private final int maxChestsPerPlayer;
    private final boolean lootProtectionEnabled;
    private final int lootProtectionMinutes;
    private final boolean hologramEnabled;
    private final String hologramText;
    private final boolean particlesEnabled;
    private final String particleType;
    private final String offlineOwnerHandling;

    private DeathChestSettings(
            boolean enabled,
            Duration despawnAfter,
            Component inventoryTitle,
            String legacyInventoryTitle,
            int maxChestsPerPlayer,
            boolean lootProtectionEnabled,
            int lootProtectionMinutes,
            boolean hologramEnabled,
            String hologramText,
            boolean particlesEnabled,
            String particleType,
            String offlineOwnerHandling
    ) {
        this.enabled = enabled;
        this.despawnAfter = despawnAfter;
        this.inventoryTitle = inventoryTitle;
        this.legacyInventoryTitle = legacyInventoryTitle;
        this.maxChestsPerPlayer = maxChestsPerPlayer;
        this.lootProtectionEnabled = lootProtectionEnabled;
        this.lootProtectionMinutes = lootProtectionMinutes;
        this.hologramEnabled = hologramEnabled;
        this.hologramText = hologramText;
        this.particlesEnabled = particlesEnabled;
        this.particleType = particleType;
        this.offlineOwnerHandling = offlineOwnerHandling;
    }

    public boolean isEnabled() {
        return enabled;
    }
    public Duration getDespawnAfter() {
        return despawnAfter;
    }
    public Component getInventoryTitle() {
        return inventoryTitle;
    }
    public String getLegacyInventoryTitle() {
        return legacyInventoryTitle;
    }
    public int getMaxChestsPerPlayer() {
        return maxChestsPerPlayer;
    }
    public boolean isLootProtectionEnabled() {
        return lootProtectionEnabled;
    }
    public int getLootProtectionMinutes() {
        return lootProtectionMinutes;
    }
    public boolean isHologramEnabled() {
        return hologramEnabled;
    }
    public String getHologramText() {
        return hologramText;
    }
    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }
    public String getParticleType() {
        return particleType;
    }
    public String getOfflineOwnerHandling() {
        return offlineOwnerHandling;
    }

    public static DeathChestSettings fromConfiguration(FileConfiguration configuration) {
        ConfigurationSection section = configuration.getConfigurationSection("death-chests");
        if (section == null) {
            return defaultSettings();
        }

        boolean enabled = section.getBoolean("enabled", false);
        long despawnMinutes = Math.max(0L, section.getLong("despawn-minutes", 30L));
        Duration despawnAfter = despawnMinutes > 0 ? Duration.ofMinutes(despawnMinutes) : Duration.ZERO;

        String rawTitle = section.getString("inventory-title", "<gold>Death Chest</gold>");
        Component inventoryTitle = parseTitle(rawTitle);
        String legacyTitle = serializeTitle(inventoryTitle, rawTitle);

        int maxChestsPerPlayer = section.getInt("max-chests-per-player", 0);
        ConfigurationSection lootSection = section.getConfigurationSection("loot-protection");
        boolean lootProtectionEnabled = lootSection != null && lootSection.getBoolean("enabled", false);
        int lootProtectionMinutes = lootSection != null ? lootSection.getInt("owner-only-minutes", 0) : 0;
        ConfigurationSection hologramSection = section.getConfigurationSection("hologram");
        boolean hologramEnabled = hologramSection != null && hologramSection.getBoolean("enabled", false);
        String hologramText = hologramSection != null ? hologramSection.getString("text", "<gold>Death Chest</gold>") : "<gold>Death Chest</gold>";
        ConfigurationSection particlesSection = section.getConfigurationSection("particles");
        boolean particlesEnabled = particlesSection != null && particlesSection.getBoolean("enabled", false);
        String particleType = particlesSection != null ? particlesSection.getString("type", "VILLAGER_HAPPY") : "VILLAGER_HAPPY";
        String offlineOwnerHandling = section.getString("offline-owner-handling", "drop");

        return new DeathChestSettings(
            enabled,
            despawnAfter,
            inventoryTitle,
            legacyTitle,
            maxChestsPerPlayer,
            lootProtectionEnabled,
            lootProtectionMinutes,
            hologramEnabled,
            hologramText,
            particlesEnabled,
            particleType,
            offlineOwnerHandling
        );
    }

    private static Component parseTitle(String input) {
        if (input == null || input.isBlank()) {
            return Component.text("Death Chest");
        }
        try {
            return MINI_MESSAGE.deserialize(input);
        } catch (Exception ex) {
            String translated = ChatColor.translateAlternateColorCodes('&', input);
            return LEGACY_SERIALIZER.deserialize(translated);
        }
    }

    private static String serializeTitle(Component component, String fallback) {
        if (component != null) {
            try {
                return LEGACY_SERIALIZER.serialize(component);
            } catch (Exception ignored) {
                // Fallback below if serialization fails.
            }
        }
        String input = fallback == null ? "Death Chest" : fallback;
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private static DeathChestSettings defaultSettings() {
        Component title = parseTitle("<gold>Death Chest</gold>");
        String legacy = serializeTitle(title, "Death Chest");
        return new DeathChestSettings(
                false,
                Duration.ofMinutes(30),
                title,
                legacy,
                0,
                false,
                0,
                false,
                "<gold>Death Chest</gold>",
                false,
                "VILLAGER_HAPPY",
                "drop"
        );
    }
}
