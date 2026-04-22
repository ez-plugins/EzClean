package com.skyblockexp.ezclean.scheduler;

import com.skyblockexp.ezclean.config.CleanupSettings;
import com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass;
import com.skyblockexp.ezclean.util.EntityPileDetector;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RemovalEvaluatorTest {

    private WorldGuardCleanupBypass bypass;

    @BeforeEach
    void setUp() {
        bypass = Mockito.mock(WorldGuardCleanupBypass.class);
    }

    private CleanupSettings createSettings(boolean removePassive, boolean removeVillagers, boolean removeVehicles) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection cleaners = config.createSection("cleaners");
        ConfigurationSection cleaner = cleaners.createSection("test");
        cleaner.set("interval-minutes", 5);
        cleaner.set("remove.hostile-mobs", false);
        cleaner.set("remove.passive-mobs", removePassive);
        cleaner.set("remove.villagers", removeVillagers);
        cleaner.set("remove.vehicles", removeVehicles);
        cleaner.set("remove.dropped-items", false);
        cleaner.set("remove.projectiles", false);
        cleaner.set("remove.experience-orbs", false);
        cleaner.set("remove.area-effect-clouds", false);
        cleaner.set("remove.falling-blocks", false);
        cleaner.set("remove.primed-tnt", false);

        List<CleanupSettings> settings = CleanupSettings.fromConfiguration(config, Logger.getLogger("test"));
        return settings.get(0);
    }

    @Test
    void worldGuardBypassSkipsRemoval() {
        CleanupSettings settings = createSettings(true, true, true);
        Item item = Mockito.mock(Item.class);
        Mockito.when(bypass.shouldBypass(item)).thenReturn(true);

        RemovalEvaluator evaluator = new RemovalEvaluator(bypass);
        assertNull(evaluator.evaluateRemovalGroup(item, settings, "", null));
    }

    @Test
    void forcedKeepAndForcedRemove() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection cleaners = config.createSection("cleaners");
        ConfigurationSection cleaner = cleaners.createSection("test");
        cleaner.set("interval-minutes", 5);
        cleaner.set("entity-types.keep", List.of("ZOMBIE"));
        cleaner.set("entity-types.remove", List.of("ITEM"));

        CleanupSettings settings = CleanupSettings.fromConfiguration(config, Logger.getLogger("test")).get(0);

        org.bukkit.entity.Zombie zombie = Mockito.mock(org.bukkit.entity.Zombie.class);
        Mockito.when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        Item item = Mockito.mock(Item.class);
        Mockito.when(item.getType()).thenReturn(EntityType.ITEM);

        RemovalEvaluator evaluator = new RemovalEvaluator(null);
        assertNull(evaluator.evaluateRemovalGroup(zombie, settings, "", null));
        assertEquals("forced-removal", evaluator.evaluateRemovalGroup(item, settings, "", null));
    }

    @Test
    void tamedMobsProtected() {
        // default settings protect tamed mobs
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection cleaners = config.createSection("cleaners");
        ConfigurationSection cleaner = cleaners.createSection("test");
        cleaner.set("interval-minutes", 5);
        CleanupSettings settings = CleanupSettings.fromConfiguration(config, Logger.getLogger("test")).get(0);

        Tameable fox = Mockito.mock(Tameable.class);
        Mockito.when(fox.isTamed()).thenReturn(true);
        Mockito.when(fox.getType()).thenReturn(EntityType.FOX);

        RemovalEvaluator evaluator = new RemovalEvaluator(null);
        assertNull(evaluator.evaluateRemovalGroup(fox, settings, "", null));
    }

    @Test
    void pileDetectorCulls() {
        CleanupSettings settings = createSettings(false, false, false);
        Item item = Mockito.mock(Item.class);
        EntityPileDetector pileDetector = Mockito.mock(EntityPileDetector.class);
        Mockito.when(pileDetector.shouldCull(item)).thenReturn(true);

        RemovalEvaluator evaluator = new RemovalEvaluator(null);
        assertEquals("pile-detection", evaluator.evaluateRemovalGroup(item, settings, "", pileDetector));
    }

    @Test
    void vehicleTypeDetection() {
        CleanupSettings settings = createSettings(false, false, true);
        Vehicle v = Mockito.mock(Vehicle.class);
        EntityType boatType = resolveVehicleType("BOAT", "MINECART", "CHEST_BOAT");
        Mockito.when(v.getType()).thenReturn(boatType);
        RemovalEvaluator evaluator = new RemovalEvaluator(null);
        assertEquals("vehicles", evaluator.evaluateRemovalGroup(v, settings, "", null));

        Entity nonVehicle = Mockito.mock(Entity.class);
        EntityType minecartType = resolveVehicleType("MINECART", "MINECART_CHEST", "MINECART_HOPPER");
        Mockito.when(nonVehicle.getType()).thenReturn(minecartType);
        assertEquals("vehicles", evaluator.evaluateRemovalGroup(nonVehicle, settings, "", null));
    }

    private EntityType resolveVehicleType(String... preferredNames) {
        for (String name : preferredNames) {
            EntityType match = findEntityType(name);
            if (match != null) {
                return match;
            }
        }

        for (EntityType type : EntityType.values()) {
            String name = type.name();
            if (name.equals("MINECART") || name.equals("BOAT") || name.equals("CHEST_BOAT")
                    || name.endsWith("_MINECART") || name.endsWith("_BOAT") || name.endsWith("_RAFT")) {
                return type;
            }
        }

        throw new IllegalStateException("No vehicle entity type is available in this server version");
    }

    private EntityType findEntityType(String name) {
        for (EntityType type : EntityType.values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Spawn-reason filter tests
    // -----------------------------------------------------------------------

    @Test
    void spawnReasonRestrictProtectsFromCategoryRemoval() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection cleaners = config.createSection("cleaners");
        ConfigurationSection cleaner = cleaners.createSection("test");
        cleaner.set("interval-minutes", 5);
        cleaner.set("remove.hostile-mobs", true);
        cleaner.set("spawn-reasons.restrict", List.of("SPAWNER"));

        CleanupSettings settings = CleanupSettings.fromConfiguration(config, Logger.getLogger("test")).get(0);

        org.bukkit.entity.Zombie zombie = Mockito.mock(org.bukkit.entity.Zombie.class);
        Mockito.when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        Mockito.when(zombie.getEntitySpawnReason())
                .thenReturn(CreatureSpawnEvent.SpawnReason.SPAWNER);

        RemovalEvaluator evaluator = new RemovalEvaluator(null);
        // Spawner zombie must be protected even though hostile-mobs removal is on.
        assertNull(evaluator.evaluateRemovalGroup(zombie, settings, "world", null));
    }

    @Test
    void spawnReasonRestrictDoesNotProtectUnmatchedReason() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection cleaners = config.createSection("cleaners");
        ConfigurationSection cleaner = cleaners.createSection("test");
        cleaner.set("interval-minutes", 5);
        cleaner.set("remove.hostile-mobs", true);
        cleaner.set("spawn-reasons.restrict", List.of("SPAWNER"));

        CleanupSettings settings = CleanupSettings.fromConfiguration(config, Logger.getLogger("test")).get(0);

        org.bukkit.entity.Zombie zombie = Mockito.mock(org.bukkit.entity.Zombie.class);
        Mockito.when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        Mockito.when(zombie.getEntitySpawnReason())
                .thenReturn(CreatureSpawnEvent.SpawnReason.NATURAL);

        RemovalEvaluator evaluator = new RemovalEvaluator(null);
        // Naturally spawned zombie has no restriction — it should be removed.
        assertEquals("hostile-mobs", evaluator.evaluateRemovalGroup(zombie, settings, "world", null));
    }

    @Test
    void spawnReasonForceRemoveBypassesNameTagProtection() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection cleaners = config.createSection("cleaners");
        ConfigurationSection cleaner = cleaners.createSection("test");
        cleaner.set("interval-minutes", 5);
        cleaner.set("remove.hostile-mobs", true);
        cleaner.set("protect.name-tagged-mobs", true);
        cleaner.set("spawn-reasons.force-remove", List.of("SPAWNER_EGG"));

        CleanupSettings settings = CleanupSettings.fromConfiguration(config, Logger.getLogger("test")).get(0);

        org.bukkit.entity.Zombie zombie = Mockito.mock(org.bukkit.entity.Zombie.class);
        Mockito.when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        Mockito.when(zombie.getCustomName()).thenReturn("Pinky");
        Mockito.when(zombie.getEntitySpawnReason())
                .thenReturn(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);

        RemovalEvaluator evaluator = new RemovalEvaluator(null);
        // Named mob but force-removed by spawn reason — must return a removal group.
        assertEquals("forced-spawn-reason", evaluator.evaluateRemovalGroup(zombie, settings, "world", null));
    }

    @Test
    void worldOverrideSpawnReasonTakesPrecedenceOverGlobal() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection cleaners = config.createSection("cleaners");
        ConfigurationSection cleaner = cleaners.createSection("test");
        cleaner.set("interval-minutes", 5);
        cleaner.set("remove.hostile-mobs", true);
        // Global: restrict SPAWNER.
        cleaner.set("spawn-reasons.restrict", List.of("SPAWNER"));
        // world_nether override: no restrictions.
        cleaner.set("world-overrides.world_nether.spawn-reasons.restrict", List.of());

        CleanupSettings settings = CleanupSettings.fromConfiguration(config, Logger.getLogger("test")).get(0);

        org.bukkit.entity.Zombie zombie = Mockito.mock(org.bukkit.entity.Zombie.class);
        Mockito.when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        Mockito.when(zombie.getEntitySpawnReason())
                .thenReturn(CreatureSpawnEvent.SpawnReason.SPAWNER);

        RemovalEvaluator evaluator = new RemovalEvaluator(null);
        // In "world": global restrict applies — protected.
        assertNull(evaluator.evaluateRemovalGroup(zombie, settings, "world", null));
        // In "world_nether": override has empty restrict list — removed.
        assertEquals("hostile-mobs", evaluator.evaluateRemovalGroup(zombie, settings, "world_nether", null));
    }
}
