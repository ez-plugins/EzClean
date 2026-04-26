package com.skyblockexp.ezclean.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable entity-protection settings for a cleanup profile.
 *
 * <p>Covers which entity categories are shielded from removal, custom name-tag pattern
 * matching, and per-item-metadata protection rules for dropped items.</p>
 */
public final class ProtectSettings {

    private final boolean protectPlayers;
    private final boolean protectArmorStands;
    private final boolean protectDisplayEntities;
    private final boolean protectTamedMobs;
    private final boolean protectNameTaggedMobs;
    private final List<Pattern> nameTagPatterns;
    private final ItemMetadataFilter itemMetadataFilter;

    ProtectSettings(boolean protectPlayers, boolean protectArmorStands, boolean protectDisplayEntities,
            boolean protectTamedMobs, boolean protectNameTaggedMobs,
            List<Pattern> nameTagPatterns, ItemMetadataFilter itemMetadataFilter) {
        this.protectPlayers = protectPlayers;
        this.protectArmorStands = protectArmorStands;
        this.protectDisplayEntities = protectDisplayEntities;
        this.protectTamedMobs = protectTamedMobs;
        this.protectNameTaggedMobs = protectNameTaggedMobs;
        this.nameTagPatterns = Collections.unmodifiableList(new ArrayList<>(nameTagPatterns));
        this.itemMetadataFilter = itemMetadataFilter;
    }

    /**
     * Loads protection settings from the {@code protect} sub-section of a cleaner
     * configuration section.
     *
     * @param section     the cleaner configuration section
     * @param logger      used to report invalid configuration values; may be {@code null}
     * @param sectionPath human-readable path used in log messages
     * @return an immutable settings instance
     */
    public static ProtectSettings load(
            ConfigurationSection section, @Nullable Logger logger, String sectionPath) {
        boolean protectPlayers = section.getBoolean("protect.players", true);
        boolean protectArmorStands = section.getBoolean("protect.armor-stands", true);
        boolean protectDisplayEntities = section.getBoolean("protect.display-entities", true);
        boolean protectTamedMobs = section.getBoolean("protect.tamed-mobs", true);
        boolean protectNameTaggedMobs = section.getBoolean("protect.name-tagged-mobs", true);

        List<Pattern> nameTagPatterns = parseNameTagPatterns(
                section.getStringList("protect.name-tag-patterns"), logger, sectionPath);
        ItemMetadataFilter itemMetadataFilter = ItemMetadataFilter.parse(
                section.getConfigurationSection("protect.item-metadata"), logger,
                sectionPath + ".protect.item-metadata");

        return new ProtectSettings(protectPlayers, protectArmorStands, protectDisplayEntities,
                protectTamedMobs, protectNameTaggedMobs, nameTagPatterns, itemMetadataFilter);
    }

    // -----------------------------------------------------------------------------------------
    // Protection flags
    // -----------------------------------------------------------------------------------------

    /** Whether players are protected from removal (should always be {@code true}). */
    public boolean protectPlayers() {
        return protectPlayers;
    }

    /** Whether armor stands are protected from removal. */
    public boolean protectArmorStands() {
        return protectArmorStands;
    }

    /** Whether display entities (text display, item display, block display) are protected. */
    public boolean protectDisplayEntities() {
        return protectDisplayEntities;
    }

    /**
     * Whether mobs that have been tamed by a player are protected from removal.
     * <p><strong>WARNING:</strong> disabling this while {@code remove.passive-mobs} is {@code true}
     * will delete player pets.</p>
     */
    public boolean protectTamedMobs() {
        return protectTamedMobs;
    }

    /**
     * Whether mobs with a custom name tag are protected from removal.
     * <p>When non-empty {@link #getNameTagPatterns() name-tag patterns} are configured, only
     * mobs whose name matches at least one pattern are protected; others are treated as unnamed.</p>
     */
    public boolean protectNameTaggedMobs() {
        return protectNameTaggedMobs;
    }

    // -----------------------------------------------------------------------------------------
    // Name tag patterns
    // -----------------------------------------------------------------------------------------

    /**
     * Returns the compiled name-tag protection patterns for this profile.
     *
     * <p>An empty list means <em>all</em> named mobs are protected (default behaviour). When
     * non-empty, only mobs whose custom name matches at least one pattern are protected by the
     * name-tagged-mobs rule.</p>
     */
    public List<Pattern> getNameTagPatterns() {
        return nameTagPatterns;
    }

    // -----------------------------------------------------------------------------------------
    // Item metadata filter
    // -----------------------------------------------------------------------------------------

    /** Returns the item-metadata protection filter for dropped items. */
    public ItemMetadataFilter getItemMetadataFilter() {
        return itemMetadataFilter;
    }

    // -----------------------------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------------------------

    private static List<Pattern> parseNameTagPatterns(
            List<String> raw, @Nullable Logger logger, String sectionPath) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<Pattern> patterns = new ArrayList<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            try {
                patterns.add(Pattern.compile(entry, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            } catch (PatternSyntaxException ex) {
                if (logger != null) {
                    logger.warning(String.format(
                            "Invalid regex in protect.name-tag-patterns at '%s': %s — skipping.",
                            sectionPath, ex.getMessage()));
                }
            }
        }
        return patterns.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(patterns);
    }
}
