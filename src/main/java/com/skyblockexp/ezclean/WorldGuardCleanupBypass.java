package com.skyblockexp.ezclean;

import java.util.logging.Logger;
import org.bukkit.entity.Entity;

/**
 * Compatibility wrapper for WorldGuardCleanupBypass moved into the `integration` package.
 *
 * @deprecated Use {@link com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass} directly.
 */
@Deprecated
public final class WorldGuardCleanupBypass {

    private final com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass delegate;

    private WorldGuardCleanupBypass(com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass delegate) {
        this.delegate = delegate;
    }

    public static void registerBypassFlag(Logger logger) {
        com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass.registerBypassFlag(logger);
    }

    public static WorldGuardCleanupBypass create(Logger logger) {
        com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass d = com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass.create(logger);
        return new WorldGuardCleanupBypass(d);
    }

    public boolean shouldBypass(Entity entity) {
        return delegate.shouldBypass(entity);
    }

    public String getFlagName() {
        return delegate.getFlagName();
    }
}
