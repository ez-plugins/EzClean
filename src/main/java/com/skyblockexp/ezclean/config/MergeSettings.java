package com.skyblockexp.ezclean.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable configuration for the entity/item merging feature.
 *
 * <p>When enabled, the cleanup pass will merge nearby identical dropped items into a single
 * entity and combine nearby experience orbs into a single orb — rather than deleting them.
 * This achieves the same entity-count reduction as removal but preserves player loot and XP.</p>
 *
 * <p>Merging runs as an early pass inside each cleanup cycle, before the normal removal
 * evaluation. Entities that are merged away count toward a {@code merged-items} or
 * {@code merged-orbs} removal group so they appear in statistics.</p>
 */
public final class MergeSettings {

    private final boolean mergeItems;
    private final boolean mergeOrbs;
    private final int radiusBlocks;
    private final int maxStackSize;
    private final boolean mergeAcrossWorlds;

    MergeSettings(boolean mergeItems, boolean mergeOrbs, int radiusBlocks, int maxStackSize) {
        this.mergeItems = mergeItems;
        this.mergeOrbs = mergeOrbs;
        this.radiusBlocks = radiusBlocks;
        this.maxStackSize = maxStackSize;
        this.mergeAcrossWorlds = false;
    }

    /**
     * Loads merge settings from the {@code merging} sub-section of a cleaner configuration.
     *
     * @param section     the cleaner configuration section
     * @return a configured instance, or {@code null} when merging is disabled or absent
     */
    public static @Nullable MergeSettings load(ConfigurationSection section) {
        ConfigurationSection merge = section.getConfigurationSection("merging");
        if (merge == null) {
            return null;
        }
        if (!merge.getBoolean("enabled", false)) {
            return null;
        }

        boolean items = merge.getBoolean("merge-items", true);
        boolean orbs = merge.getBoolean("merge-experience-orbs", true);
        int radius = Math.max(1, merge.getInt("radius-blocks", 3));
        // Max stack size override — 0 means "use each item type's vanilla max stack size".
        int maxStack = Math.max(0, merge.getInt("max-stack-size", 0));

        if (!items && !orbs) {
            return null;
        }

        return new MergeSettings(items, orbs, radius, maxStack);
    }

    /** Whether dropped items should be merged. */
    public boolean isMergeItems() {
        return mergeItems;
    }

    /** Whether experience orbs should be merged. */
    public boolean isMergeOrbs() {
        return mergeOrbs;
    }

    /** Radius in blocks within which two entities of the same type are eligible for merging. */
    public int getRadiusBlocks() {
        return radiusBlocks;
    }

    /**
     * Override maximum stack size (items only). When {@code 0} the vanilla per-material max is used.
     * When positive, stacks are limited to this value regardless of material.
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }
}
