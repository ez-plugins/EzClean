package com.skyblockexp.ezclean.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Spark profiler integration settings for a cleanup profile.
 *
 * <p>When {@link #isAutoProfileEnabled()} is {@code true} and the Spark plugin is
 * installed, EzClean automatically starts a Spark profiler session whenever the number
 * of entities scheduled for removal in a single cleanup run reaches
 * {@link #getAutoProfileThreshold()}.  The session runs for
 * {@link #getAutoProfileDurationSeconds()} seconds and then automatically stops and
 * uploads a shareable report link to the console, helping server staff identify the
 * root source of entity buildup without manual profiling.</p>
 *
 * <p>Auto-profiling is <strong>disabled by default</strong>.  Enable it under
 * {@code performance.spark.auto-profile} in the cleaner YAML file.</p>
 */
public final class SparkSettings {

    private static final SparkSettings DISABLED = new SparkSettings(false, 1000, 30);

    private final boolean autoProfileEnabled;
    private final int autoProfileThreshold;
    private final int autoProfileDurationSeconds;

    SparkSettings(boolean autoProfileEnabled, int autoProfileThreshold, int autoProfileDurationSeconds) {
        this.autoProfileEnabled = autoProfileEnabled;
        this.autoProfileThreshold = Math.max(1, autoProfileThreshold);
        this.autoProfileDurationSeconds = Math.max(5, autoProfileDurationSeconds);
    }

    /**
     * Loads Spark settings from the {@code performance.spark} sub-section of a cleaner
     * configuration section. Missing keys fall back to sensible defaults.
     *
     * @param section the cleaner configuration section
     * @return an immutable settings instance
     */
    public static SparkSettings load(ConfigurationSection section) {
        ConfigurationSection sparkSection = section.getConfigurationSection("performance.spark");
        if (sparkSection == null) {
            return DISABLED;
        }
        ConfigurationSection profileSection = sparkSection.getConfigurationSection("auto-profile");
        if (profileSection == null) {
            return DISABLED;
        }
        boolean enabled = profileSection.getBoolean("enabled", false);
        int threshold = Math.max(1, profileSection.getInt("threshold", 1000));
        int duration = Math.max(5, profileSection.getInt("duration-seconds", 30));
        return new SparkSettings(enabled, threshold, duration);
    }

    /** Returns a settings instance with auto-profiling disabled. */
    public static SparkSettings disabled() {
        return DISABLED;
    }

    /** Whether automatic Spark profiling on high entity counts is active for this profile. */
    public boolean isAutoProfileEnabled() {
        return autoProfileEnabled;
    }

    /**
     * Minimum number of entities scheduled for removal in a single cleanup run to
     * trigger automatic Spark profiling.
     */
    public int getAutoProfileThreshold() {
        return autoProfileThreshold;
    }

    /** Duration in seconds for the automatic Spark profiling session. */
    public int getAutoProfileDurationSeconds() {
        return autoProfileDurationSeconds;
    }
}
