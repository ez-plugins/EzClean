package com.skyblockexp.ezclean.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable configuration for pile-detection cleanup passes.
 *
 * <p>Pile detection automatically culls excessive stacks of the same entity type that
 * share a single block position, complementing the category-based removal rules.</p>
 */
public final class PileDetectionSettings {

    private final int maxPerBlock;
    private final Set<EntityType> trackedTypes;
    private final boolean ignoreNamedEntities;

    PileDetectionSettings(int maxPerBlock, Set<EntityType> trackedTypes, boolean ignoreNamedEntities) {
        this.maxPerBlock = maxPerBlock;
        this.trackedTypes = trackedTypes;
        this.ignoreNamedEntities = ignoreNamedEntities;
    }

    /**
     * Loads pile-detection settings from the {@code pile-detection} sub-section of a cleaner
     * configuration section.
     *
     * @param section     the cleaner configuration section
     * @param logger      used to report invalid configuration values; may be {@code null}
     * @param sectionPath human-readable path for log messages
     * @return a configured instance, or {@code null} when pile-detection is disabled or absent
     */
    public static @Nullable PileDetectionSettings load(
            ConfigurationSection section, @Nullable Logger logger, String sectionPath) {
        ConfigurationSection pileSection = section.getConfigurationSection("pile-detection");
        if (pileSection == null) {
            return null;
        }

        if (!pileSection.getBoolean("enabled", false)) {
            return null;
        }

        int threshold = pileSection.getInt("max-per-block", 200);
        if (threshold <= 0) {
            if (logger != null) {
                logger.warning(() -> String.format(
                        "Ignoring pile-detection for '%s': max-per-block must be greater than zero.",
                        sectionPath));
            }
            return null;
        }

        boolean ignoreNamed = pileSection.getBoolean("ignore-named-entities", true);
        Set<EntityType> tracked = new HashSet<>(
                parseEntityTypes(pileSection.getStringList("entity-types"), logger,
                        sectionPath + ".pile-detection.entity-types"));
        if (tracked.isEmpty()) {
            tracked.add(EntityType.ITEM);
            tracked.add(EntityType.EXPERIENCE_ORB);
        }

        return new PileDetectionSettings(threshold, Collections.unmodifiableSet(tracked), ignoreNamed);
    }

    /** Maximum entities of a tracked type that may share one block before extras are culled. */
    public int getMaxPerBlock() {
        return maxPerBlock;
    }

    /** Entity types monitored for pile detection. */
    public Set<EntityType> getTrackedTypes() {
        return trackedTypes;
    }

    /** Whether named entities are excluded from pile-detection culling. */
    public boolean ignoreNamedEntities() {
        return ignoreNamedEntities;
    }

    /** Returns {@code true} when {@code type} is included in pile-detection tracking. */
    public boolean isTracking(EntityType type) {
        return trackedTypes.contains(type);
    }

    // -----------------------------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------------------------

    private static Set<EntityType> parseEntityTypes(
            List<String> entries, @Nullable Logger logger, String path) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptySet();
        }
        Set<EntityType> result = new HashSet<>();
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                result.add(EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                if (logger != null) {
                    logger.warning(String.format("Unknown entity type '%s' at '%s'; skipping.", raw, path));
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
