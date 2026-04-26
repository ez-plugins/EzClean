package com.skyblockexp.ezclean.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable configuration for per-chunk entity cap enforcement.
 *
 * <p>When enabled, each cleanup pass tracks entity counts per chunk. Once a chunk exceeds the
 * configured cap for a tracked entity category, excess entities are removed in addition to
 * (and before) the normal category-based cleanup rules.</p>
 *
 * <p>Unlike pile-detection (which fires only when many entities share a single block), chunk
 * caps enforce a hard ceiling across the whole 16×16 chunk column, preventing lag build-up
 * between cleanup intervals.</p>
 */
public final class ChunkCapSettings {

    /** Entity categories that can be capped per chunk. */
    public enum Category {
        HOSTILE_MOBS,
        PASSIVE_MOBS,
        DROPPED_ITEMS,
        EXPERIENCE_ORBS,
        ALL
    }

    private final int maxEntitiesPerChunk;
    private final Set<Category> trackedCategories;
    private final Set<EntityType> ignoredTypes;
    private final boolean ignoreNamedEntities;

    ChunkCapSettings(int maxEntitiesPerChunk, Set<Category> trackedCategories,
            Set<EntityType> ignoredTypes, boolean ignoreNamedEntities) {
        this.maxEntitiesPerChunk = maxEntitiesPerChunk;
        this.trackedCategories = Collections.unmodifiableSet(new HashSet<>(trackedCategories));
        this.ignoredTypes = Collections.unmodifiableSet(new HashSet<>(ignoredTypes));
        this.ignoreNamedEntities = ignoreNamedEntities;
    }

    /**
     * Loads chunk-cap settings from the {@code chunk-caps} sub-section of a cleaner configuration.
     *
     * @param section     the cleaner configuration section
     * @param logger      used to report invalid values; may be {@code null}
     * @param sectionPath human-readable path used in log messages
     * @return a configured instance, or {@code null} when chunk-caps is disabled or absent
     */
    public static @Nullable ChunkCapSettings load(
            ConfigurationSection section, @Nullable Logger logger, String sectionPath) {
        ConfigurationSection caps = section.getConfigurationSection("chunk-caps");
        if (caps == null) {
            return null;
        }
        if (!caps.getBoolean("enabled", false)) {
            return null;
        }

        int max = caps.getInt("max-entities-per-chunk", 150);
        if (max <= 0) {
            if (logger != null) {
                logger.warning(() -> String.format(
                        "Ignoring chunk-caps for '%s': max-entities-per-chunk must be greater than zero.",
                        sectionPath));
            }
            return null;
        }

        Set<Category> categories = parseCategories(caps.getStringList("categories"), logger, sectionPath);
        if (categories.isEmpty()) {
            categories = EnumSet.of(Category.HOSTILE_MOBS, Category.PASSIVE_MOBS);
        }

        Set<EntityType> ignored = parseEntityTypes(caps.getStringList("ignore-types"), logger, sectionPath);
        boolean ignoreNamed = caps.getBoolean("ignore-named-entities", true);

        return new ChunkCapSettings(max, categories, ignored, ignoreNamed);
    }

    /** Maximum total entities of tracked categories allowed per chunk before extras are removed. */
    public int getMaxEntitiesPerChunk() {
        return maxEntitiesPerChunk;
    }

    /** Categories monitored for chunk-cap enforcement. */
    public Set<Category> getTrackedCategories() {
        return trackedCategories;
    }

    /** Entity types explicitly excluded from chunk-cap counts. */
    public Set<EntityType> getIgnoredTypes() {
        return ignoredTypes;
    }

    /** When {@code true}, entities with a custom name are not counted toward the chunk cap. */
    public boolean ignoreNamedEntities() {
        return ignoreNamedEntities;
    }

    /** Returns {@code true} if the given category is tracked by these settings. */
    public boolean isTracking(Category category) {
        return trackedCategories.contains(Category.ALL) || trackedCategories.contains(category);
    }

    // -----------------------------------------------------------------------------------------
    // Parsing helpers
    // -----------------------------------------------------------------------------------------

    private static Set<Category> parseCategories(List<String> names, @Nullable Logger logger, String path) {
        Set<Category> result = new HashSet<>();
        for (String name : names) {
            try {
                result.add(Category.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                if (logger != null) {
                    logger.warning(() -> String.format(
                            "Unknown chunk-cap category '%s' in '%s' — valid values: %s",
                            name, path, List.of(Category.values())));
                }
            }
        }
        return result;
    }

    private static Set<EntityType> parseEntityTypes(List<String> names, @Nullable Logger logger, String path) {
        Set<EntityType> result = new HashSet<>();
        for (String name : names) {
            try {
                result.add(EntityType.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                if (logger != null) {
                    logger.warning(() -> String.format("Unknown entity type '%s' in '%s'.", name, path));
                }
            }
        }
        return result;
    }
}
