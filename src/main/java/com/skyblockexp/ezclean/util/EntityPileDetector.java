package com.skyblockexp.ezclean.util;

import com.skyblockexp.ezclean.config.CleanupSettings;
import com.skyblockexp.ezclean.config.PileDetectionSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks how many configured entities occupy the same block and flags overflow entries for removal.
 */
public final class EntityPileDetector {

    private final @Nullable PileDetectionSettings settings;
    private final Map<PileKey, Integer> counts = new HashMap<>();
    private final Map<String, Integer> worldCounts = new HashMap<>();
    private final Map<ChunkKey, Integer> chunkCounts = new HashMap<>();

    private EntityPileDetector(@Nullable PileDetectionSettings settings) {
        this.settings = settings;
    }

    public static @Nullable EntityPileDetector create(@Nullable PileDetectionSettings settings) {
        if (settings == null) {
            return null;
        }
        return new EntityPileDetector(settings);
    }

    public static EntityPileDetector createSummaryTracker(@Nullable PileDetectionSettings settings) {
        return new EntityPileDetector(settings);
    }

    public boolean shouldCull(Entity entity) {
        if (settings == null) {
            return false;
        }
        EntityType type = entity.getType();
        if (!settings.isTracking(type)) {
            return false;
        }
        if (settings.ignoreNamedEntities()) {
            String customName = entity.getCustomName();
            if (customName != null && !customName.isBlank()) {
                return false;
            }
        }

        Location location = entity.getLocation();
        PileKey key = new PileKey(entity.getWorld().getUID(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ(), type);
        int total = counts.merge(key, 1, Integer::sum);
        return total > settings.getMaxPerBlock();
    }

    public void recordEntity(Entity entity) {
        Location location = entity.getLocation();
        String worldName = entity.getWorld().getName();
        worldCounts.merge(worldName, 1, Integer::sum);
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        chunkCounts.merge(new ChunkKey(worldName, chunkX, chunkZ), 1, Integer::sum);
    }

    public List<WorldCount> getTopWorlds(int limit) {
        return getTopWorldEntries(limit).stream()
                .map(entry -> new WorldCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<ChunkCount> getTopChunks(int limit) {
        return getTopChunkEntries(limit).stream()
                .map(entry -> new ChunkCount(entry.getKey().worldName(), entry.getKey().chunkX(),
                        entry.getKey().chunkZ(), entry.getValue()))
                .toList();
    }

    private List<Map.Entry<String, Integer>> getTopWorldEntries(int limit) {
        if (worldCounts.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<Map.Entry<String, Integer>> results = new ArrayList<>(worldCounts.entrySet());
        results.sort(Comparator
                .comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue())
                .reversed()
                .thenComparing(Map.Entry::getKey));
        if (results.size() <= limit) {
            return results;
        }
        return results.subList(0, limit);
    }

    private List<Map.Entry<ChunkKey, Integer>> getTopChunkEntries(int limit) {
        if (chunkCounts.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<Map.Entry<ChunkKey, Integer>> results = new ArrayList<>(chunkCounts.entrySet());
        results.sort(Comparator
                .comparingInt((Map.Entry<ChunkKey, Integer> entry) -> entry.getValue())
                .reversed()
                .thenComparing(entry -> entry.getKey().worldName())
                .thenComparingInt(entry -> entry.getKey().chunkX())
                .thenComparingInt(entry -> entry.getKey().chunkZ()));
        if (results.size() <= limit) {
            return results;
        }
        return results.subList(0, limit);
    }

    private record PileKey(UUID worldId, int blockX, int blockY, int blockZ, EntityType type) {

        private PileKey {
            Objects.requireNonNull(worldId, "worldId");
            Objects.requireNonNull(type, "type");
        }
    }

    private record ChunkKey(String worldName, int chunkX, int chunkZ) {

        private ChunkKey {
            Objects.requireNonNull(worldName, "worldName");
        }
    }

    public record WorldCount(String worldName, int count) {
    }

    public record ChunkCount(String worldName, int chunkX, int chunkZ, int count) {
    }
}
