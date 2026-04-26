package com.skyblockexp.ezclean.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Immutable TPS/MSPT-aware deferral settings for a cleanup profile.
 *
 * <p>When enabled (default), cleanup runs are postponed while the server TPS falls below
 * {@link #getMinTps()} or MSPT exceeds {@link #getMaxMspt()}. Deferral is capped at
 * {@link #getMaxDeferSeconds()} to prevent indefinite postponement on persistently laggy
 * servers.</p>
 */
public final class TpsAwareSettings {

    private static final TpsAwareSettings DEFAULTS = new TpsAwareSettings(true, 16.0, 50.0, 120);

    private final boolean enabled;
    private final double minTps;
    private final double maxMspt;
    private final int maxDeferSeconds;

    TpsAwareSettings(boolean enabled, double minTps, double maxMspt, int maxDeferSeconds) {
        this.enabled = enabled;
        this.minTps = minTps;
        this.maxMspt = maxMspt;
        this.maxDeferSeconds = maxDeferSeconds;
    }

    /**
     * Loads TPS-aware settings from the {@code performance.tps-aware} sub-section of a
     * cleaner configuration section. Missing keys fall back to sensible defaults.
     *
     * @param section the cleaner configuration section
     * @return an immutable settings instance
     */
    public static TpsAwareSettings load(ConfigurationSection section) {
        boolean enabled = section.getBoolean("performance.tps-aware.enabled", true);
        double minTps = Math.max(0.0, section.getDouble("performance.tps-aware.min-tps", 16.0));
        double maxMspt = Math.max(0.0, section.getDouble("performance.tps-aware.max-mspt", 50.0));
        int maxDeferSeconds = Math.max(0, section.getInt("performance.tps-aware.max-defer-seconds", 120));
        return new TpsAwareSettings(enabled, minTps, maxMspt, maxDeferSeconds);
    }

    /** Returns the default TPS-aware settings (enabled with recommended thresholds). */
    public static TpsAwareSettings defaults() {
        return DEFAULTS;
    }

    /** Whether TPS/MSPT-aware deferral is active for this profile. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Minimum 1-minute TPS; cleanup is deferred if TPS falls below this value. */
    public double getMinTps() {
        return minTps;
    }

    /** Maximum milliseconds-per-tick; cleanup is deferred if MSPT exceeds this value. */
    public double getMaxMspt() {
        return maxMspt;
    }

    /** Maximum seconds to keep deferring before forcing a run regardless of server load. */
    public int getMaxDeferSeconds() {
        return maxDeferSeconds;
    }
}
