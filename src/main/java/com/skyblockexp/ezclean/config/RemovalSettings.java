package com.skyblockexp.ezclean.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Immutable entity-removal settings for a cleanup profile.
 *
 * <p>Covers which entity categories are removed during a cleanup pass, plus age-based and
 * player-proximity gates for dropped items and experience orbs.</p>
 */
public final class RemovalSettings {

    private final boolean removeHostileMobs;
    private final boolean removePassiveMobs;
    private final boolean removeVillagers;
    private final boolean removeVehicles;
    private final boolean removeDroppedItems;
    private final boolean removeProjectiles;
    private final boolean removeExperienceOrbs;
    private final boolean removeAreaEffectClouds;
    private final boolean removeFallingBlocks;
    private final boolean removePrimedTnt;
    private final int droppedItemsMinAgeTicks;
    private final int experienceOrbsMinAgeTicks;
    private final int itemPlayerProximityBlocks;
    private final int orbPlayerProximityBlocks;

    RemovalSettings(boolean removeHostileMobs, boolean removePassiveMobs, boolean removeVillagers,
            boolean removeVehicles, boolean removeDroppedItems, boolean removeProjectiles,
            boolean removeExperienceOrbs, boolean removeAreaEffectClouds, boolean removeFallingBlocks,
            boolean removePrimedTnt, int droppedItemsMinAgeTicks, int experienceOrbsMinAgeTicks,
            int itemPlayerProximityBlocks, int orbPlayerProximityBlocks) {
        this.removeHostileMobs = removeHostileMobs;
        this.removePassiveMobs = removePassiveMobs;
        this.removeVillagers = removeVillagers;
        this.removeVehicles = removeVehicles;
        this.removeDroppedItems = removeDroppedItems;
        this.removeProjectiles = removeProjectiles;
        this.removeExperienceOrbs = removeExperienceOrbs;
        this.removeAreaEffectClouds = removeAreaEffectClouds;
        this.removeFallingBlocks = removeFallingBlocks;
        this.removePrimedTnt = removePrimedTnt;
        this.droppedItemsMinAgeTicks = Math.max(0, droppedItemsMinAgeTicks);
        this.experienceOrbsMinAgeTicks = Math.max(0, experienceOrbsMinAgeTicks);
        this.itemPlayerProximityBlocks = Math.max(0, itemPlayerProximityBlocks);
        this.orbPlayerProximityBlocks = Math.max(0, orbPlayerProximityBlocks);
    }

    /**
     * Loads removal settings from the {@code remove} and {@code protect.player-proximity}
     * sub-sections of a cleaner configuration section.
     *
     * @param section the cleaner configuration section
     * @return an immutable settings instance
     */
    public static RemovalSettings load(ConfigurationSection section) {
        return new RemovalSettings(
                section.getBoolean("remove.hostile-mobs", true),
                section.getBoolean("remove.passive-mobs", false),
                section.getBoolean("remove.villagers", false),
                section.getBoolean("remove.vehicles", false),
                section.getBoolean("remove.dropped-items", true),
                section.getBoolean("remove.projectiles", true),
                section.getBoolean("remove.experience-orbs", true),
                section.getBoolean("remove.area-effect-clouds", true),
                section.getBoolean("remove.falling-blocks", true),
                section.getBoolean("remove.primed-tnt", true),
                section.getInt("remove.dropped-items-min-age-ticks", 0),
                section.getInt("remove.experience-orbs-min-age-ticks", 0),
                section.getInt("protect.player-proximity.dropped-items", 0),
                section.getInt("protect.player-proximity.experience-orbs", 0));
    }

    // -----------------------------------------------------------------------------------------
    // Category flags
    // -----------------------------------------------------------------------------------------

    /** Whether hostile (monster) mobs are removed during cleanup. */
    public boolean removeHostileMobs() {
        return removeHostileMobs;
    }

    /**
     * Whether passive mobs (farm animals, golems, ambient, aquatic) are removed during cleanup.
     * <p><strong>WARNING:</strong> enabling this alongside {@code protectTamedMobs = false} will
     * delete player pets.</p>
     */
    public boolean removePassiveMobs() {
        return removePassiveMobs;
    }

    /** Whether villagers and wandering traders are removed during cleanup. */
    public boolean removeVillagers() {
        return removeVillagers;
    }

    /** Whether vehicles (boats, minecarts) are removed during cleanup. */
    public boolean removeVehicles() {
        return removeVehicles;
    }

    /** Whether dropped items on the ground are removed during cleanup. */
    public boolean removeDroppedItems() {
        return removeDroppedItems;
    }

    /** Whether projectiles (arrows, snowballs, etc.) are removed during cleanup. */
    public boolean removeProjectiles() {
        return removeProjectiles;
    }

    /** Whether experience orbs are removed during cleanup. */
    public boolean removeExperienceOrbs() {
        return removeExperienceOrbs;
    }

    /** Whether area-effect clouds are removed during cleanup. */
    public boolean removeAreaEffectClouds() {
        return removeAreaEffectClouds;
    }

    /** Whether falling block entities are removed during cleanup. */
    public boolean removeFallingBlocks() {
        return removeFallingBlocks;
    }

    /** Whether primed TNT entities are removed during cleanup. */
    public boolean removePrimedTnt() {
        return removePrimedTnt;
    }

    // -----------------------------------------------------------------------------------------
    // Age / proximity gates
    // -----------------------------------------------------------------------------------------

    /**
     * Minimum age in ticks a dropped item must have accumulated before it is eligible for
     * removal. {@code 0} means items are removed regardless of age.
     */
    public int getDroppedItemsMinAgeTicks() {
        return droppedItemsMinAgeTicks;
    }

    /**
     * Minimum age in ticks an experience orb must have accumulated before it is eligible for
     * removal. {@code 0} means orbs are removed regardless of age.
     */
    public int getExperienceOrbsMinAgeTicks() {
        return experienceOrbsMinAgeTicks;
    }

    /**
     * Block-distance radius around each online player within which dropped items are protected
     * from cleanup. {@code 0} disables proximity protection.
     */
    public int getItemPlayerProximityBlocks() {
        return itemPlayerProximityBlocks;
    }

    /**
     * Block-distance radius around each online player within which experience orbs are protected
     * from cleanup. {@code 0} disables proximity protection.
     */
    public int getOrbPlayerProximityBlocks() {
        return orbPlayerProximityBlocks;
    }
}
