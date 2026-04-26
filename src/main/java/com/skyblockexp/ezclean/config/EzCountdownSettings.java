package com.skyblockexp.ezclean.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable EzCountdown integration settings for a cleanup profile.
 *
 * <p>When enabled, EzClean mirrors each cleanup countdown to EzCountdown so that the
 * remaining time is displayed via EzCountdown's display system (action bar, boss bar,
 * title, scoreboard, etc.).</p>
 */
public final class EzCountdownSettings {

    private static final EzCountdownSettings DISABLED = new EzCountdownSettings(
            false, "ezclean-{cleaner}", List.of("ACTION_BAR"),
            "<yellow>Entity cleanup in <gold><time_left></gold></yellow>",
            null, null, null, 1);

    private final boolean enabled;
    private final String countdownNameTemplate;
    private final List<String> displayTypes;
    private final String formatMessage;
    private final @Nullable String startMessage;
    private final @Nullable String endMessage;
    private final @Nullable String visibilityPermission;
    private final int updateIntervalSeconds;

    EzCountdownSettings(boolean enabled, String countdownNameTemplate, List<String> displayTypes,
            String formatMessage, @Nullable String startMessage, @Nullable String endMessage,
            @Nullable String visibilityPermission, int updateIntervalSeconds) {
        this.enabled = enabled;
        this.countdownNameTemplate = countdownNameTemplate;
        this.displayTypes = Collections.unmodifiableList(new ArrayList<>(displayTypes));
        this.formatMessage = formatMessage;
        this.startMessage = startMessage;
        this.endMessage = endMessage;
        this.visibilityPermission = visibilityPermission;
        this.updateIntervalSeconds = updateIntervalSeconds;
    }

    /**
     * Returns a disabled EzCountdown settings instance. Used when no
     * {@code integrations.ezcountdown} block is present in the config.
     */
    public static EzCountdownSettings disabled() {
        return DISABLED;
    }

    /**
     * Loads EzCountdown integration settings from the {@code integrations.ezcountdown}
     * sub-section of a cleaner configuration section.
     *
     * @param section the cleaner configuration section
     * @return an immutable settings instance (disabled when the block is absent)
     */
    public static EzCountdownSettings load(ConfigurationSection section) {
        ConfigurationSection ezSection = section.getConfigurationSection("integrations.ezcountdown");
        if (ezSection == null) {
            return DISABLED;
        }

        boolean enabled = ezSection.getBoolean("enabled", false);
        String nameTemplate = "ezclean-{cleaner}";
        String configName = ezSection.getString("countdown-name");
        if (configName != null && !configName.isBlank()) {
            nameTemplate = configName;
        }

        List<String> displayTypes = Collections.singletonList("ACTION_BAR");
        List<String> configuredTypes = ezSection.getStringList("display-types");
        if (!configuredTypes.isEmpty()) {
            displayTypes = configuredTypes;
        }

        String formatMessage = "<yellow>Entity cleanup in <gold><time_left></gold></yellow>";
        String configFormat = ezSection.getString("format-message");
        if (configFormat != null && !configFormat.isBlank()) {
            formatMessage = configFormat;
        }

        String startMessage = nullIfBlank(ezSection.getString("start-message"));
        String endMessage = nullIfBlank(ezSection.getString("end-message"));
        String visibilityPermission = nullIfBlank(ezSection.getString("visibility-permission"));
        int updateIntervalSeconds = Math.max(1, ezSection.getInt("update-interval-seconds", 1));

        return new EzCountdownSettings(enabled, nameTemplate, displayTypes, formatMessage,
                startMessage, endMessage, visibilityPermission, updateIntervalSeconds);
    }

    /** Whether the EzCountdown integration is active for this profile. */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the countdown name with {@code {cleaner}} replaced by the given cleaner identifier.
     */
    public String resolveCountdownName(String cleanerId) {
        return countdownNameTemplate.replace("{cleaner}", cleanerId);
    }

    /** EzCountdown display types to use (e.g. {@code ACTION_BAR}, {@code BOSS_BAR}). */
    public List<String> getDisplayTypes() {
        return displayTypes;
    }

    /** Format string passed to EzCountdown for countdown display rendering. */
    public String getFormatMessage() {
        return formatMessage;
    }

    /** Optional message broadcast by EzCountdown when the countdown starts, or {@code null}. */
    public @Nullable String getStartMessage() {
        return startMessage;
    }

    /** Optional message broadcast by EzCountdown when the countdown ends, or {@code null}. */
    public @Nullable String getEndMessage() {
        return endMessage;
    }

    /** Permission required to see the countdown; {@code null} means visible to all players. */
    public @Nullable String getVisibilityPermission() {
        return visibilityPermission;
    }

    /** How often (seconds) EzCountdown refreshes its display for this countdown. */
    public int getUpdateIntervalSeconds() {
        return updateIntervalSeconds;
    }

    private static @Nullable String nullIfBlank(@Nullable String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
