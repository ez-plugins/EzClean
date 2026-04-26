package com.skyblockexp.ezclean.util;

import com.skyblockexp.ezclean.config.ChunkCapSettings;
import com.skyblockexp.ezclean.config.ChunkCapSettings.Category;

import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.WaterMob;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks entity counts per chunk and flags entities that push a chunk over its configured cap.
 *
 * <p>During each cleanup pass, every entity in enabled worlds is registered with
 * {@link #shouldCap(Entity)}, which returns {@code true} the moment adding that entity would
 * exceed the configured {@code max-entities-per-chunk} value for its category. The first N
 * entities of each category in each chunk are kept; subsequent ones are flagged for removal.</p>
 *
 * <p>This complements pile-detection (which targets same-block stacks) and category-level
 * cleanup (which is category-wide) by providing a per-chunk ceiling that fires regardless of
 * how spread-out the entities are within the chunk.</p>
 */
public final class ChunkCapTracker {

    private final ChunkCapSettings settings;
    /**
     * Maps {@code worldUID:chunkX:chunkZ:category} → current entity count within that
     * chunk for that category. Populated incrementally as entities are evaluated.
     */
    private final Map<ChunkCategoryKey, Integer> counts = new HashMap<>();

    public ChunkCapTracker(ChunkCapSettings settings) {
        this.settings = settings;
    }

    /**
     * Evaluates whether the given entity pushes its chunk over the configured cap.
     *
     * @param entity the entity to evaluate
     * @return {@code true} if this entity should be removed due to the chunk cap
     */
    public boolean shouldCap(Entity entity) {
        if (settings.getIgnoredTypes().contains(entity.getType())) {
            return false;
        }
        if (settings.ignoreNamedEntities()) {
            String name = entity.getCustomName();
            if (name != null && !name.isBlank()) {
                return false;
            }
        }

        Category category = resolveCategory(entity);
        if (category == null || !settings.isTracking(category)) {
            // Also check ALL category explicitly since isTracking covers that.
            if (!settings.isTracking(Category.ALL)) {
                return false;
            }
        }

        Location loc = entity.getLocation();
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        UUID worldId = entity.getWorld().getUID();

        ChunkCategoryKey key = new ChunkCategoryKey(worldId, chunkX, chunkZ,
                category != null ? category : Category.ALL);
        int count = counts.merge(key, 1, Integer::sum);
        return count > settings.getMaxEntitiesPerChunk();
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    /**
     * Maps an entity to its {@link Category}, or returns {@code null} if none of the tracked
     * categories apply (i.e. the entity is not relevant to chunk-cap enforcement).
     */
    private static @org.jetbrains.annotations.Nullable Category resolveCategory(Entity entity) {
        if (entity instanceof Enemy) {
            return Category.HOSTILE_MOBS;
        }
        if (entity instanceof Item) {
            return Category.DROPPED_ITEMS;
        }
        if (entity instanceof ExperienceOrb) {
            return Category.EXPERIENCE_ORBS;
        }
        if (entity instanceof Animals || entity instanceof WaterMob || entity instanceof Ambient
                || entity instanceof Golem || entity instanceof Allay
                || (entity instanceof Mob && !(entity instanceof Enemy))) {
            return Category.PASSIVE_MOBS;
        }
        return null;
    }

    // -----------------------------------------------------------------------------------------
    // Key record
    // -----------------------------------------------------------------------------------------

    private record ChunkCategoryKey(UUID worldId, int chunkX, int chunkZ, Category category) {}
}
