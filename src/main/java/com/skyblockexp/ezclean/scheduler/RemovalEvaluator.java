package com.skyblockexp.ezclean.scheduler;

import java.util.EnumSet;

import com.skyblockexp.ezclean.config.SpawnReasonFilter;
import com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass;
import com.skyblockexp.ezclean.config.CleanupSettings;
import com.skyblockexp.ezclean.util.EntityPileDetector;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Animals;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.Enemy;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates entity removal evaluation logic so it can be unit tested separately from scheduling.
 */
public final class RemovalEvaluator {

    private static final EnumSet<EntityType> BASIC_VEHICLE_TYPES = createBasicVehicleTypes();

    private final @Nullable WorldGuardCleanupBypass worldGuardBypass;

    public RemovalEvaluator(@Nullable WorldGuardCleanupBypass worldGuardBypass) {
        this.worldGuardBypass = worldGuardBypass;
    }

    public @Nullable String evaluateRemovalGroup(Entity entity, CleanupSettings settings,
            String worldName, @Nullable EntityPileDetector pileDetector) {
        if (worldGuardBypass != null && worldGuardBypass.shouldBypass(entity)) {
            return null;
        }

        if (settings.isForcedKeep(entity.getType())) {
            return null;
        }
        if (settings.isForcedRemoval(entity.getType())) {
            return "forced-removal";
        }
        // Players are always protected — no config option can override this.
        if (entity instanceof Player && settings.protectPlayers()) {
            return null;
        }

        // Spawn-reason filter: force-remove bypasses the protect rules below.
        SpawnReasonFilter spawnFilter = settings.getSpawnReasonFilter(worldName);
        if (!spawnFilter.isEmpty()) {
            CreatureSpawnEvent.SpawnReason spawnReason = entity.getEntitySpawnReason();
            if (spawnFilter.isForceRemove(spawnReason)) {
                return "forced-spawn-reason";
            }
        }

        if (entity instanceof org.bukkit.entity.ArmorStand && settings.protectArmorStands()) {
            return null;
        }
        if (entity instanceof org.bukkit.entity.Display && settings.protectDisplayEntities()) {
            return null;
        }
        if (entity instanceof Tameable tameable && tameable.isTamed() && settings.protectTamedMobs()) {
            return null;
        }
        if (entity instanceof Mob mob && settings.protectNameTaggedMobs()) {
            String customName = mob.getCustomName();
            if (customName != null && !customName.isBlank()) {
                return null;
            }
        }

        // Spawn-reason filter: restrict shields the entity from category-based removal.
        if (!spawnFilter.isEmpty()) {
            CreatureSpawnEvent.SpawnReason spawnReason = entity.getEntitySpawnReason();
            if (spawnFilter.isRestricted(spawnReason)) {
                return null;
            }
        }
        if (entity instanceof Enemy && settings.removeHostileMobs()) {
            return "hostile-mobs";
        }
        if (entity instanceof AbstractVillager && settings.removeVillagers()) {
            return "villagers";
        }
        if (settings.removePassiveMobs()) {
            if (entity instanceof Animals || entity instanceof WaterMob || entity instanceof Ambient
                    || entity instanceof Golem || entity instanceof Allay
                    || (entity instanceof Mob && !(entity instanceof Enemy) && !(entity instanceof AbstractVillager))) {
                return "passive-mobs";
            }
        }
        if (settings.removeVehicles() && isVehicleEntity(entity)) {
            return "vehicles";
        }
        if (entity instanceof Item && settings.removeDroppedItems()) {
            return "dropped-items";
        }
        if (entity instanceof org.bukkit.entity.ExperienceOrb && settings.removeExperienceOrbs()) {
            return "experience-orbs";
        }
        if (entity instanceof Projectile && settings.removeProjectiles()) {
            return "projectiles";
        }
        if (entity instanceof AreaEffectCloud && settings.removeAreaEffectClouds()) {
            return "area-effect-clouds";
        }
        if (entity instanceof FallingBlock && settings.removeFallingBlocks()) {
            return "falling-blocks";
        }
        if (entity instanceof TNTPrimed && settings.removePrimedTnt()) {
            return "primed-tnt";
        }
        if (pileDetector != null && pileDetector.shouldCull(entity)) {
            return "pile-detection";
        }
        return null;
    }

    public static boolean isVehicleEntity(Entity entity) {
        if (entity instanceof Vehicle) {
            return true;
        }
        EntityType type = entity.getType();
        if (type == null) {
            return false;
        }
        if (BASIC_VEHICLE_TYPES.contains(type)) {
            return true;
        }
        String typeName = type.name();
        return typeName.contains("MINECART");
    }

    private static EnumSet<EntityType> createBasicVehicleTypes() {
        EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);
        for (EntityType type : EntityType.values()) {
            String name = type.name();
            if (name.equals("MINECART") || name.equals("BOAT") || name.equals("CHEST_BOAT")
                    || name.endsWith("_MINECART") || name.endsWith("_BOAT")
                    || name.endsWith("_RAFT")) {
                types.add(type);
            }
        }
        return types;
    }
}
