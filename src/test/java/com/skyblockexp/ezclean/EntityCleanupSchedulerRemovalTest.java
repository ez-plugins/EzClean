package com.skyblockexp.ezclean;

import com.skyblockexp.ezclean.config.CleanupSettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vehicle;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EntityCleanupSchedulerRemovalTest {

    private JavaPlugin plugin;

    private WorldGuardCleanupBypass worldGuardBypass;
    private CleanupStatsTracker statsTracker;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(JavaPlugin.class);
        worldGuardBypass = Mockito.mock(WorldGuardCleanupBypass.class);
        statsTracker = Mockito.mock(CleanupStatsTracker.class);
    }

        @Test
        void passiveMobsRequireExplicitToggle() {
        com.skyblockexp.ezclean.config.CleanupSettings disabledSettings = createSettings(false, false, false);
        com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler scheduler = new com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler(plugin,
            Collections.singletonList(disabledSettings), worldGuardBypass, statsTracker, null);

        Animals cow = Mockito.mock(Animals.class);
        Mockito.when(cow.getType()).thenReturn(EntityType.COW);

        assertFalse(shouldRemove(scheduler, cow, disabledSettings),
            "Passive mobs should be preserved when the toggle is disabled");

        com.skyblockexp.ezclean.config.CleanupSettings enabledSettings = createSettings(true, false, false);
        scheduler = new com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler(plugin, Collections.singletonList(enabledSettings), worldGuardBypass, statsTracker, null);

        Animals sheep = Mockito.mock(Animals.class);
        Mockito.when(sheep.getType()).thenReturn(EntityType.SHEEP);

        assertTrue(shouldRemove(scheduler, sheep, enabledSettings),
            "Passive mobs should be culled when the toggle is enabled");
        }

        @Test
        void villagersRequireDedicatedToggle() {
        com.skyblockexp.ezclean.config.CleanupSettings passiveEnabledVillagersDisabled = createSettings(true, false, false);
        com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler scheduler = new com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler(plugin,
            Collections.singletonList(passiveEnabledVillagersDisabled), worldGuardBypass, statsTracker, null);

        AbstractVillager villager = Mockito.mock(AbstractVillager.class);
        Mockito.when(villager.getType()).thenReturn(EntityType.VILLAGER);

        assertFalse(shouldRemove(scheduler, villager, passiveEnabledVillagersDisabled),
            "Villagers should not be culled unless the villager toggle is enabled");

        com.skyblockexp.ezclean.config.CleanupSettings villagersEnabled = createSettings(false, true, false);
        scheduler = new com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler(plugin, Collections.singletonList(villagersEnabled), worldGuardBypass, statsTracker, null);

        AbstractVillager trader = Mockito.mock(AbstractVillager.class);
        Mockito.when(trader.getType()).thenReturn(EntityType.WANDERING_TRADER);

        assertTrue(shouldRemove(scheduler, trader, villagersEnabled),
            "Villagers and traders should be culled when the villager toggle is enabled");
        }

        @Test
        void vehiclesRequireExplicitToggle() {
        com.skyblockexp.ezclean.config.CleanupSettings disabledSettings = createSettings(false, false, false);
        com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler scheduler = new com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler(plugin,
            Collections.singletonList(disabledSettings), worldGuardBypass, statsTracker, null);

        Vehicle boat = Mockito.mock(Vehicle.class);
        Mockito.when(boat.getType()).thenReturn(resolveVehicleType("BOAT"));

        assertFalse(shouldRemove(scheduler, boat, disabledSettings),
            "Vehicles should be preserved when the toggle is disabled");

        com.skyblockexp.ezclean.config.CleanupSettings enabledSettings = createSettings(false, false, true);
        scheduler = new com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler(plugin, Collections.singletonList(enabledSettings), worldGuardBypass, statsTracker, null);

        Vehicle minecart = Mockito.mock(Vehicle.class);
        Mockito.when(minecart.getType()).thenReturn(EntityType.MINECART);

        assertTrue(shouldRemove(scheduler, minecart, enabledSettings),
            "Vehicles should be culled when the toggle is enabled");
        }

        @Test
        void removesVehicleTypesWithoutVehicleInterface() {
        com.skyblockexp.ezclean.config.CleanupSettings enabledSettings = createSettings(false, false, true);
        com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler scheduler = new com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler(plugin,
            Collections.singletonList(enabledSettings), worldGuardBypass, statsTracker, null);

        EntityType vehicleType = resolveVehicleType("CHEST_BOAT", "MINECART_CHEST", "MINECART_HOPPER",
            "MINECART_FURNACE", "MINECART_TNT", "MINECART");
        Entity chestBoat = Mockito.mock(Entity.class);
        Mockito.when(chestBoat.getType()).thenReturn(vehicleType);

        assertTrue(shouldRemove(scheduler, chestBoat, enabledSettings),
            "Vehicle entity types should be culled even without the Vehicle interface");
        }
    /**
     * Helper to check if an entity would be removed by the scheduler.
     */
    private boolean shouldRemove(com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler scheduler, Entity entity, com.skyblockexp.ezclean.config.CleanupSettings settings) {
        try {
            var method = com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler.class.getDeclaredMethod("evaluateRemovalGroup", Entity.class, com.skyblockexp.ezclean.config.CleanupSettings.class, String.class, Class.forName("com.skyblockexp.ezclean.util.EntityPileDetector"));
            method.setAccessible(true);
            Object result = method.invoke(scheduler, entity, settings, "", null);
            return result != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        List<com.skyblockexp.ezclean.config.CleanupSettings> settings = com.skyblockexp.ezclean.config.CleanupSettings.fromConfiguration(config, Logger.getLogger("test"));
        return settings.get(0);
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
}
