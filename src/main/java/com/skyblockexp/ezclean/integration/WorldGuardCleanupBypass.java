package com.skyblockexp.ezclean.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Provides optional WorldGuard-backed bypass checks so regions can opt out of EzClean removals.
 */
public final class WorldGuardCleanupBypass {

    private static final String FLAG_NAME = "ezclean-bypass";

    private static volatile StateFlag registeredFlag;

    private final Logger logger;
    private final RegionContainer regionContainer;
    private final StateFlag bypassFlag;

    private WorldGuardCleanupBypass(Logger logger, RegionContainer regionContainer, StateFlag bypassFlag) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.regionContainer = Objects.requireNonNull(regionContainer, "regionContainer");
        this.bypassFlag = Objects.requireNonNull(bypassFlag, "bypassFlag");
    }

    /**
     * Attempts to create a WorldGuard bypass helper, registering the EzClean flag if necessary.
     *
     * @param logger the plugin logger used for integration messages
     * @return a configured bypass helper
     */
    public static WorldGuardCleanupBypass create(Logger logger) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) {
            throw new IllegalStateException("WorldGuard region container is unavailable.");
        }

        StateFlag bypassFlag = resolveRegisteredFlag();
        return new WorldGuardCleanupBypass(logger, container, bypassFlag);
    }

    /**
     * Ensures the EzClean WorldGuard flag is registered prior to plugin enable.
     *
     * @param logger the plugin logger used for integration messages
     */
    public static void registerBypassFlag(Logger logger) {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        Flag<?> existing = registry.get(FLAG_NAME);

        if (existing == null) {
            StateFlag flag = new StateFlag(FLAG_NAME, false);
            try {
                registry.register(flag);
                registeredFlag = flag;
                logger.info(() -> String.format(
                        "Registered WorldGuard flag '%s' for EzClean bypass support.", FLAG_NAME));
                return;
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Unable to register EzClean WorldGuard flag '" + FLAG_NAME + "'.", ex);
            }
        }

        if (existing instanceof StateFlag stateFlag) {
            registeredFlag = stateFlag;
            logger.info(() -> String.format(
                    "Using pre-existing WorldGuard flag '%s' for EzClean bypass support.", FLAG_NAME));
            return;
        }

        throw new IllegalStateException(
                "WorldGuard flag '" + FLAG_NAME + "' is already registered but is not a state flag.");
    }

    private static StateFlag resolveRegisteredFlag() {
        StateFlag flag = registeredFlag;
        if (flag != null) {
            return flag;
        }

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        Flag<?> existing = registry.get(FLAG_NAME);
        if (existing instanceof StateFlag stateFlag) {
            registeredFlag = stateFlag;
            return stateFlag;
        }

        throw new IllegalStateException(
                "WorldGuard flag '" + FLAG_NAME + "' has not been registered yet or is not a state flag.");
    }

    /**
     * Determines if the provided entity is inside a region where cleanups should be bypassed.
     *
     * @param entity the entity being considered for removal
     * @return {@code true} if the entity resides in a bypassed region, {@code false} otherwise
     */
    public boolean shouldBypass(Entity entity) {
        Location location = entity.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        RegionManager manager = getRegionManager(world);
        if (manager == null) {
            return false;
        }

        try {
            ApplicableRegionSet regions = manager.getApplicableRegions(BlockVector3.at(location.getBlockX(),
                    location.getBlockY(), location.getBlockZ()));
            if (regions == null) {
                return false;
            }
            StateFlag.State state = regions.queryState(null, bypassFlag);
            return state == StateFlag.State.ALLOW;
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "Failed to query WorldGuard regions for EzClean bypass check.", ex);
            return false;
        }
    }

    private RegionManager getRegionManager(World world) {
        try {
            return regionContainer.get(BukkitAdapter.adapt(world));
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "Unable to access WorldGuard region manager for world " + world.getName(), ex);
            return null;
        }
    }

    /**
     * @return the name of the WorldGuard state flag that enables cleanup bypass behaviour.
     */
    public String getFlagName() {
        return bypassFlag.getName();
    }
}
