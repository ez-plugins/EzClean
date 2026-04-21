package com.skyblockexp.ezclean.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable view of the EzClean configuration options.
 */
public final class CleanupSettings {

    private static final long TICKS_PER_MINUTE = 20L * 60L;
    private static final String DEFAULT_WARNING_MESSAGE =
            "<yellow>⚠ Entity cleanup in <gold>{minutes}</gold> minutes. Clear valuables!</yellow>";
    private static final String DEFAULT_START_MESSAGE = "<red><bold>✦ Entity cleanup commencing...</bold></red>";
    private static final String DEFAULT_SUMMARY_MESSAGE =
            "<gray>✓ Removed <gold>{count}</gold> entities. Next cleanup in <gold>{minutes}</gold> minutes.</gray>";
    private static final String DEFAULT_INTERVAL_MESSAGE =
            "<yellow>⚠ Entity cleanup in <gold>{minutes}</gold> minutes. Clear valuables!</yellow>";
    private static final String DEFAULT_DYNAMIC_MESSAGE =
            "<yellow>⚠ Entity cleanup in <gold>{minutes}</gold> minutes. Clear valuables!</yellow>";
    private static final String DEFAULT_STATS_SUMMARY_MESSAGE =
            "<gray>Cleanup stats for <aqua>{cleaner}</aqua>: <gold>{runs}</gold> runs, "
                    + "<gold>{total_removed}</gold> removed total. Avg duration: <gold>{avg_duration}</gold>. "
                    + "Avg TPS impact: <gold>{avg_tps_impact}</gold>. Top groups: {top_groups}. "
                    + "Top worlds: {top_worlds}.</gray>";
    private static final String DEFAULT_CANCEL_HOVER_MESSAGE =
            "<yellow>Click to pay <gold>{cost}</gold> to cancel this cleanup.</yellow>";
    private static final String DEFAULT_CANCEL_SUCCESS_MESSAGE =
            "<green>You paid <gold>{cost}</gold> to cancel the <aqua>{cleaner}</aqua> cleanup.</green>";
    private static final String DEFAULT_CANCEL_BROADCAST_MESSAGE =
            "<gold>{player}</gold> canceled the <aqua>{cleaner}</aqua> cleanup. Next cleanup in <yellow>{minutes}</yellow> minutes.";
    private static final String DEFAULT_CANCEL_INSUFFICIENT_FUNDS_MESSAGE =
            "<red>You need <gold>{cost}</gold> to cancel the cleanup.</red>";
    private static final String DEFAULT_CANCEL_DISABLED_MESSAGE =
            "<red>This cleanup cannot be canceled.</red>";
    private static final String DEFAULT_CANCEL_NO_ECONOMY_MESSAGE =
            "<red>Economy is unavailable. Cleanup cannot be canceled.</red>";

    private final String cleanerId;
    private final long cleanupIntervalTicks;
    private final long warningOffsetTicks;
    private final boolean warningEnabled;
    private final boolean startBroadcastEnabled;
    private final boolean summaryBroadcastEnabled;
    private final boolean intervalBroadcastEnabled;
    private final long intervalBroadcastMinutes;
    private final String intervalBroadcastMessageTemplate;
    private final boolean dynamicBroadcastEnabled;
    private final Set<Long> dynamicBroadcastMinutes;
    private final Set<Long> dynamicBroadcastSeconds;
    private final String dynamicBroadcastMessageTemplate;
    private final boolean statsSummaryBroadcastEnabled;
    private final long statsSummaryEveryRuns;
    private final String statsSummaryMessageTemplate;
    private final String warningMessageTemplate;
    private final String startMessageTemplate;
    private final String summaryMessageTemplate;
    private final @Nullable String preCleanMessageTemplate;
    private final long warningMinutesBefore;
    private final long cleanupIntervalMinutes;
    private final boolean removeHostileMobs;
    private final boolean removePassiveMobs;
    private final boolean removeVillagers;
    private final boolean removeVehicles;
    private final boolean removeDroppedItems;
    private final boolean removeProjectiles;
    private final boolean removeExperienceOrbs;
    private final boolean removeAreaEffectClouds;
    private final boolean removeFallingBlocks;
    private final boolean removePrimedTnt;
    private final boolean protectPlayers;
    private final boolean protectArmorStands;
    private final boolean protectDisplayEntities;
    private final boolean protectTamedMobs;
    private final boolean protectNameTaggedMobs;
    private final Set<String> enabledWorlds;
    private final Set<EntityType> forcedKeeps;
    private final Set<EntityType> forcedRemovals;
    private final @Nullable PileDetectionSettings pileDetectionSettings;
    private final CleanupCancelSettings cancelSettings;
    private final boolean asyncRemoval;

    private static final Pattern MINIMESSAGE_PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_-]+)\\}");

    private CleanupSettings(String cleanerId, long cleanupIntervalTicks, long warningOffsetTicks, boolean warningEnabled,
            boolean startBroadcastEnabled, boolean summaryBroadcastEnabled, boolean intervalBroadcastEnabled,
            long intervalBroadcastMinutes, String intervalBroadcastMessageTemplate, boolean dynamicBroadcastEnabled,
            Set<Long> dynamicBroadcastMinutes, Set<Long> dynamicBroadcastSeconds, String dynamicBroadcastMessageTemplate,
            boolean statsSummaryBroadcastEnabled, long statsSummaryEveryRuns, String statsSummaryMessageTemplate,
            String warningMessageTemplate, String startMessageTemplate, String summaryMessageTemplate,
            @Nullable String preCleanMessageTemplate, long warningMinutesBefore,
            long cleanupIntervalMinutes, boolean removeHostileMobs, boolean removePassiveMobs,
            boolean removeVillagers, boolean removeVehicles, boolean removeDroppedItems,
            boolean removeProjectiles, boolean removeExperienceOrbs, boolean removeAreaEffectClouds,
            boolean removeFallingBlocks, boolean removePrimedTnt, boolean protectPlayers,
            boolean protectArmorStands, boolean protectDisplayEntities, boolean protectTamedMobs,
            boolean protectNameTaggedMobs, Set<String> enabledWorlds,
            Set<EntityType> forcedKeeps, Set<EntityType> forcedRemovals,
            @Nullable PileDetectionSettings pileDetectionSettings, CleanupCancelSettings cancelSettings,
            boolean asyncRemoval) {
        this.cleanerId = cleanerId;
        this.cleanupIntervalTicks = cleanupIntervalTicks;
        this.warningOffsetTicks = warningOffsetTicks;
        this.warningEnabled = warningEnabled;
        this.startBroadcastEnabled = startBroadcastEnabled;
        this.summaryBroadcastEnabled = summaryBroadcastEnabled;
        this.intervalBroadcastEnabled = intervalBroadcastEnabled;
        this.intervalBroadcastMinutes = intervalBroadcastMinutes;
        this.intervalBroadcastMessageTemplate = intervalBroadcastMessageTemplate;
        this.dynamicBroadcastEnabled = dynamicBroadcastEnabled;
        this.dynamicBroadcastMinutes = Collections.unmodifiableSet(new LinkedHashSet<>(dynamicBroadcastMinutes));
        this.dynamicBroadcastSeconds = Collections.unmodifiableSet(new LinkedHashSet<>(dynamicBroadcastSeconds));
        this.dynamicBroadcastMessageTemplate = dynamicBroadcastMessageTemplate;
        this.statsSummaryBroadcastEnabled = statsSummaryBroadcastEnabled;
        this.statsSummaryEveryRuns = statsSummaryEveryRuns;
        this.statsSummaryMessageTemplate = statsSummaryMessageTemplate;
        this.warningMessageTemplate = warningMessageTemplate;
        this.startMessageTemplate = startMessageTemplate;
        this.summaryMessageTemplate = summaryMessageTemplate;
        this.preCleanMessageTemplate = preCleanMessageTemplate;
        this.warningMinutesBefore = warningMinutesBefore;
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;
        this.removeHostileMobs = removeHostileMobs;
        this.removePassiveMobs = removePassiveMobs;
        this.removeVillagers = removeVillagers;
        this.removeVehicles = removeVehicles;
        this.removeDroppedItems = removeDroppedItems;
        this.removeProjectiles = removeProjectiles;
        this.removeExperienceOrbs = removeExperienceOrbs;
        this.removeAreaEffectClouds = removeAreaEffectClouds;
        this.removeFallingBlocks = removeFallingBlocks;
        this.removePrimedTnt = removePrimedTnt;
        this.protectPlayers = protectPlayers;
        this.protectArmorStands = protectArmorStands;
        this.protectDisplayEntities = protectDisplayEntities;
        this.protectTamedMobs = protectTamedMobs;
        this.protectNameTaggedMobs = protectNameTaggedMobs;
        this.enabledWorlds = Collections.unmodifiableSet(new HashSet<>(enabledWorlds));
        this.forcedKeeps = Collections.unmodifiableSet(new HashSet<>(forcedKeeps));
        this.forcedRemovals = Collections.unmodifiableSet(new HashSet<>(forcedRemovals));
        this.pileDetectionSettings = pileDetectionSettings;
        this.cancelSettings = cancelSettings;
        this.asyncRemoval = asyncRemoval;
    }

    /**
     * Loads cleanup settings from the provided configuration file.
     *
     * @param config the configuration to read from
     * @param logger the plugin logger used to report invalid entries
     * @return an immutable settings instance
     */
    public static List<CleanupSettings> fromConfiguration(FileConfiguration config, Logger logger) {
        ConfigurationSection cleanersSection = config.getConfigurationSection("cleaners");
        MessageConfiguration messages = MessageConfiguration.from(config.getConfigurationSection("messages"));
        if (cleanersSection == null || cleanersSection.getKeys(false).isEmpty()) {
            return Collections.singletonList(loadLegacySettings(config, logger, messages));
        }

        List<CleanupSettings> results = new ArrayList<>();
        for (String cleanerId : cleanersSection.getKeys(false)) {
            ConfigurationSection cleanerSection = cleanersSection.getConfigurationSection(cleanerId);
            if (cleanerSection == null) {
                continue;
            }
            results.add(loadFromSection(cleanerId, cleanerSection, logger, messages));
        }

        if (results.isEmpty()) {
            results.add(loadLegacySettings(config, logger, messages));
        }

        return Collections.unmodifiableList(results);
    }

    private static CleanupSettings loadLegacySettings(FileConfiguration config, Logger logger, MessageConfiguration messages) {
        ConfigurationSection cleanupSection = config.getConfigurationSection("cleanup");
        if (cleanupSection == null) {
            cleanupSection = new MemoryConfiguration();
        }
        return loadFromSection("default", cleanupSection, logger, messages);
    }

    private static CleanupSettings loadFromSection(String cleanerId, ConfigurationSection section, Logger logger,
            MessageConfiguration messages) {
        long intervalMinutes = Math.max(1L, section.getLong("interval-minutes", 60L));
        long warningMinutes = Math.max(0L, section.getLong("warning.minutes-before", 5L));
        boolean warningEnabled = section.getBoolean("warning.enabled", warningMinutes > 0L);
        boolean startEnabled = section.getBoolean("broadcast.start.enabled", true);
        boolean summaryEnabled = section.getBoolean("broadcast.summary.enabled", true);

        ConfigurationSection intervalSection = section.getConfigurationSection("broadcast.interval");
        boolean intervalEnabled = false;
        long intervalMinutesBetweenBroadcasts = 0L;
        if (intervalSection != null) {
            intervalEnabled = intervalSection.getBoolean("enabled", false);
            intervalMinutesBetweenBroadcasts = Math.max(1L, intervalSection.getLong("every-minutes", 15L));
            if (intervalMinutesBetweenBroadcasts <= 0L) {
                intervalEnabled = false;
            }
        }
        String intervalMessage = resolveMessage(cleanerId, section, messages, "broadcast.interval.message",
                DEFAULT_INTERVAL_MESSAGE);

        ConfigurationSection dynamicSection = section.getConfigurationSection("broadcast.dynamic");
        boolean dynamicEnabled = false;
        Set<Long> dynamicMinutes = Collections.emptySet();
        Set<Long> dynamicSeconds = Collections.emptySet();
        if (dynamicSection != null) {
            dynamicEnabled = dynamicSection.getBoolean("enabled", false);
            List<Integer> configuredMinutes = dynamicSection.getIntegerList("minutes");
            Set<Long> parsedMinutes = new LinkedHashSet<>();
            for (Integer value : configuredMinutes) {
                if (value == null) {
                    continue;
                }
                long minute = value.longValue();
                if (minute < 0L) {
                    continue;
                }
                parsedMinutes.add(minute);
            }
            if (!parsedMinutes.isEmpty()) {
                dynamicMinutes = Collections.unmodifiableSet(parsedMinutes);
            } else {
                dynamicEnabled = false;
            }
            List<Integer> configuredSeconds = dynamicSection.getIntegerList("seconds");
            Set<Long> parsedSeconds = new LinkedHashSet<>();
            for (Integer value : configuredSeconds) {
                if (value == null) {
                    continue;
                }
                long second = value.longValue();
                if (second < 0L) {
                    continue;
                }
                parsedSeconds.add(second);
            }
            if (!parsedSeconds.isEmpty()) {
                dynamicSeconds = Collections.unmodifiableSet(parsedSeconds);
            }
        }
        String dynamicMessage = resolveMessage(cleanerId, section, messages, "broadcast.dynamic.message",
            DEFAULT_DYNAMIC_MESSAGE);

        ConfigurationSection statsSummarySection = section.getConfigurationSection("broadcast.stats-summary");
        boolean statsSummaryEnabled = false;
        long statsSummaryEveryRuns = 0L;
        if (statsSummarySection != null) {
            statsSummaryEnabled = statsSummarySection.getBoolean("enabled", false);
            statsSummaryEveryRuns = Math.max(1L, statsSummarySection.getLong("every-runs", 5L));
            if (statsSummaryEveryRuns <= 0L) {
                statsSummaryEnabled = false;
            }
        }
        String statsSummaryMessage = resolveMessage(cleanerId, section, messages, "stats-summary.message",
                DEFAULT_STATS_SUMMARY_MESSAGE);

        String warningMessage = resolveMessage(cleanerId, section, messages, "warning.message", DEFAULT_WARNING_MESSAGE);
        String startMessage = resolveMessage(cleanerId, section, messages, "broadcast.start.message", DEFAULT_START_MESSAGE);
        String summaryMessage = resolveMessage(cleanerId, section, messages, "broadcast.summary.message",
            DEFAULT_SUMMARY_MESSAGE);
        String preCleanMessage = resolveOptionalMessage(cleanerId, section, messages, "broadcast.pre-clean.message");

        boolean removeHostileMobs = section.getBoolean("remove.hostile-mobs", true);
        boolean removePassiveMobs = section.getBoolean("remove.passive-mobs", false);
        boolean removeVillagers = section.getBoolean("remove.villagers", false);
        boolean removeVehicles = section.getBoolean("remove.vehicles", false);
        boolean removeDroppedItems = section.getBoolean("remove.dropped-items", true);
        boolean removeProjectiles = section.getBoolean("remove.projectiles", true);
        boolean removeExperienceOrbs = section.getBoolean("remove.experience-orbs", true);
        boolean removeAreaEffectClouds = section.getBoolean("remove.area-effect-clouds", true);
        boolean removeFallingBlocks = section.getBoolean("remove.falling-blocks", true);
        boolean removePrimedTnt = section.getBoolean("remove.primed-tnt", true);

        boolean protectPlayers = section.getBoolean("protect.players", true);
        boolean protectArmorStands = section.getBoolean("protect.armor-stands", true);
        boolean protectDisplayEntities = section.getBoolean("protect.display-entities", true);
        boolean protectTamedMobs = section.getBoolean("protect.tamed-mobs", true);
        boolean protectNameTaggedMobs = section.getBoolean("protect.name-tagged-mobs", true);

        Set<String> worlds = parseWorlds(section.getStringList("worlds"));

        String sectionPath = section.getCurrentPath();
        if (sectionPath == null || sectionPath.isBlank()) {
            sectionPath = "cleanup";
        }

        Set<EntityType> keep = parseEntityTypes(section.getStringList("entity-types.keep"), logger,
                sectionPath + ".entity-types.keep");
        Set<EntityType> remove = parseEntityTypes(section.getStringList("entity-types.remove"), logger,
                sectionPath + ".entity-types.remove");

        PileDetectionSettings pileDetectionSettings = loadPileDetectionSettings(section, logger, sectionPath);
        boolean asyncRemoval = section.getBoolean("performance.async-removal", false);

        com.skyblockexp.ezclean.config.CleanupCancelSettings cancelSettings = com.skyblockexp.ezclean.config.CleanupCancelSettings.disabled();
        ConfigurationSection cancelSection = section.getConfigurationSection("cancel");
        if (cancelSection != null) {
            boolean cancelEnabled = cancelSection.getBoolean("enabled", true);
            double cancelCost = cancelSection.getDouble("cost", 0.0D);
            String hoverMessage = resolveMessage(cleanerId, section, messages, "cancel.hover-message",
                    DEFAULT_CANCEL_HOVER_MESSAGE);
            String successMessage = resolveMessage(cleanerId, section, messages, "cancel.success-message",
                    DEFAULT_CANCEL_SUCCESS_MESSAGE);
            String broadcastMessage = resolveMessage(cleanerId, section, messages, "cancel.broadcast-message",
                    DEFAULT_CANCEL_BROADCAST_MESSAGE);
            String insufficientFundsMessage = resolveMessage(cleanerId, section, messages,
                    "cancel.insufficient-funds-message", DEFAULT_CANCEL_INSUFFICIENT_FUNDS_MESSAGE);
            String disabledMessage = resolveMessage(cleanerId, section, messages, "cancel.disabled-message",
                    DEFAULT_CANCEL_DISABLED_MESSAGE);
            String noEconomyMessage = resolveMessage(cleanerId, section, messages, "cancel.no-economy-message",
                    DEFAULT_CANCEL_NO_ECONOMY_MESSAGE);
            cancelSettings = CleanupCancelSettings.create(cancelEnabled, cancelCost, hoverMessage, successMessage,
                    broadcastMessage, insufficientFundsMessage, disabledMessage, noEconomyMessage);
        }

        long cleanupIntervalTicks = intervalMinutes * TICKS_PER_MINUTE;
        long warningOffsetTicks = warningMinutes * TICKS_PER_MINUTE;

        return new CleanupSettings(cleanerId, cleanupIntervalTicks, warningOffsetTicks, warningEnabled, startEnabled,
                summaryEnabled, intervalEnabled, intervalMinutesBetweenBroadcasts, intervalMessage, dynamicEnabled,
                dynamicMinutes, dynamicSeconds, dynamicMessage, statsSummaryEnabled, statsSummaryEveryRuns, statsSummaryMessage,
                warningMessage, startMessage, summaryMessage, preCleanMessage, warningMinutes, intervalMinutes,
                removeHostileMobs, removePassiveMobs, removeVillagers, removeVehicles, removeDroppedItems,
                removeProjectiles, removeExperienceOrbs, removeAreaEffectClouds, removeFallingBlocks, removePrimedTnt,
                protectPlayers, protectArmorStands, protectDisplayEntities, protectTamedMobs, protectNameTaggedMobs,
                worlds, keep, remove, pileDetectionSettings, cancelSettings, asyncRemoval);
    }

    private static String resolveMessage(String cleanerId, ConfigurationSection section, MessageConfiguration messages,
            String path, String defaultValue) {
        String value = section.getString(path);
        if (value == null || value.isEmpty()) {
            value = messages.getMessage(cleanerId, path);
        }
        if (value == null || value.isEmpty()) {
            value = defaultValue;
        }
        return normalizeMiniMessagePlaceholders(value);
    }

    private static @Nullable PileDetectionSettings loadPileDetectionSettings(ConfigurationSection section, Logger logger,
            String sectionPath) {
        ConfigurationSection pileSection = section.getConfigurationSection("pile-detection");
        if (pileSection == null) {
            return null;
        }

        boolean enabled = pileSection.getBoolean("enabled", false);
        if (!enabled) {
            return null;
        }

        int threshold = pileSection.getInt("max-per-block", 200);
        if (threshold <= 0) {
            if (logger != null) {
                logger.warning(() -> String.format(
                        "Ignoring pile-detection for '%s': max-per-block must be greater than zero.", sectionPath));
            }
            return null;
        }

        boolean ignoreNamed = pileSection.getBoolean("ignore-named-entities", true);

        Set<EntityType> trackedTypes = new HashSet<>(
                parseEntityTypes(pileSection.getStringList("entity-types"), logger,
                        sectionPath + ".pile-detection.entity-types"));
        if (trackedTypes.isEmpty()) {
            trackedTypes.add(EntityType.ITEM);
            trackedTypes.add(EntityType.EXPERIENCE_ORB);
        }

        return new PileDetectionSettings(threshold, Collections.unmodifiableSet(trackedTypes), ignoreNamed);
    }

        private static @Nullable String resolveOptionalMessage(String cleanerId, ConfigurationSection section,
            MessageConfiguration messages, String path) {
        String value = section.getString(path);
        if (value == null || value.isEmpty()) {
            value = messages.getMessage(cleanerId, path);
        }
        if (value == null || value.isEmpty()) {
            return null;
        }
        return normalizeMiniMessagePlaceholders(value);
    }

    private static final class MessageConfiguration {

        private final ConfigurationSection defaultsSection;
        private final ConfigurationSection cleanersSection;

        private MessageConfiguration(ConfigurationSection defaultsSection, ConfigurationSection cleanersSection) {
            this.defaultsSection = defaultsSection;
            this.cleanersSection = cleanersSection;
        }

        static MessageConfiguration from(@Nullable ConfigurationSection messagesSection) {
            if (messagesSection == null) {
                return new MessageConfiguration(null, null);
            }
            ConfigurationSection defaults = messagesSection.getConfigurationSection("defaults");
            ConfigurationSection cleaners = messagesSection.getConfigurationSection("cleaners");
            return new MessageConfiguration(defaults, cleaners);
        }

        String getMessage(String cleanerId, String path) {
            String value = null;
            if (cleanersSection != null) {
                ConfigurationSection cleaner = cleanersSection.getConfigurationSection(cleanerId);
                if (cleaner != null) {
                    value = cleaner.getString(path);
                }
            }
            if ((value == null || value.isEmpty()) && defaultsSection != null) {
                value = defaultsSection.getString(path);
            }
            return value;
        }
    }

    private static String normalizeMiniMessagePlaceholders(String template) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher matcher = MINIMESSAGE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = "<" + matcher.group(1) + ">";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static Set<String> parseWorlds(List<String> rawWorlds) {
        if (rawWorlds == null || rawWorlds.isEmpty()) {
            return Collections.singleton("*");
        }
        Set<String> results = new HashSet<>();
        for (String world : rawWorlds) {
            if (world == null || world.isBlank()) {
                continue;
            }
            results.add(world.toLowerCase(Locale.ROOT));
        }
        if (results.isEmpty()) {
            return Collections.singleton("*");
        }
        return Collections.unmodifiableSet(results);
    }

    private static Set<EntityType> parseEntityTypes(List<String> entries, Logger logger, String path) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptySet();
        }
        Set<EntityType> result = new HashSet<>();
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                EntityType type = EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
                result.add(type);
            } catch (IllegalArgumentException ex) {
                if (logger != null) {
                    logger.warning("Unknown entity type configured at '" + path + "': " + raw);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public String getCleanerId() {
        return cleanerId;
    }

    public long getCleanupIntervalTicks() {
        return cleanupIntervalTicks;
    }

    public long getWarningOffsetTicks() {
        return warningOffsetTicks;
    }

    public boolean isWarningEnabled() {
        return warningEnabled;
    }

    public boolean isStartBroadcastEnabled() {
        return startBroadcastEnabled;
    }

    public boolean isSummaryBroadcastEnabled() {
        return summaryBroadcastEnabled;
    }

    public boolean isIntervalBroadcastEnabled() {
        return intervalBroadcastEnabled;
    }

    public long getIntervalBroadcastMinutes() {
        return intervalBroadcastMinutes;
    }

    public String getIntervalBroadcastMessageTemplate() {
        return intervalBroadcastMessageTemplate;
    }

    public boolean isDynamicBroadcastEnabled() {
        return dynamicBroadcastEnabled;
    }

    public Set<Long> getDynamicBroadcastMinutes() {
        return dynamicBroadcastMinutes;
    }

    public Set<Long> getDynamicBroadcastSeconds() {
        return dynamicBroadcastSeconds;
    }

    public String getDynamicBroadcastMessageTemplate() {
        return dynamicBroadcastMessageTemplate;
    }

    public boolean isStatsSummaryBroadcastEnabled() {
        return statsSummaryBroadcastEnabled;
    }

    public long getStatsSummaryEveryRuns() {
        return statsSummaryEveryRuns;
    }

    public String getStatsSummaryMessageTemplate() {
        return statsSummaryMessageTemplate;
    }

    public String getWarningMessageTemplate() {
        return warningMessageTemplate;
    }

    public String getStartMessageTemplate() {
        return startMessageTemplate;
    }

    public String getSummaryMessageTemplate() {
        return summaryMessageTemplate;
    }

    public @Nullable String getPreCleanMessageTemplate() {
        return preCleanMessageTemplate;
    }

    public long getWarningMinutesBefore() {
        return warningMinutesBefore;
    }

    public long getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }

    public boolean removeHostileMobs() {
        return removeHostileMobs;
    }

    public boolean removePassiveMobs() {
        return removePassiveMobs;
    }

    public boolean removeVillagers() {
        return removeVillagers;
    }

    public boolean removeVehicles() {
        return removeVehicles;
    }

    public boolean removeDroppedItems() {
        return removeDroppedItems;
    }

    public boolean removeProjectiles() {
        return removeProjectiles;
    }

    public boolean removeExperienceOrbs() {
        return removeExperienceOrbs;
    }

    public boolean removeAreaEffectClouds() {
        return removeAreaEffectClouds;
    }

    public boolean removeFallingBlocks() {
        return removeFallingBlocks;
    }

    public boolean removePrimedTnt() {
        return removePrimedTnt;
    }

    public boolean protectPlayers() {
        return protectPlayers;
    }

    public boolean protectArmorStands() {
        return protectArmorStands;
    }

    public boolean protectDisplayEntities() {
        return protectDisplayEntities;
    }

    public boolean protectTamedMobs() {
        return protectTamedMobs;
    }

    public boolean protectNameTaggedMobs() {
        return protectNameTaggedMobs;
    }

    public boolean isWorldEnabled(String worldName) {
        if (enabledWorlds.contains("*")) {
            return true;
        }
        return enabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    public Set<String> getEnabledWorlds() {
        return enabledWorlds;
    }

    public boolean isForcedKeep(EntityType type) {
        return forcedKeeps.contains(type);
    }

    public boolean isForcedRemoval(EntityType type) {
        return forcedRemovals.contains(type);
    }

    public boolean isPileDetectionEnabled() {
        return pileDetectionSettings != null;
    }

    public @Nullable PileDetectionSettings getPileDetectionSettings() {
        return pileDetectionSettings;
    }

    public CleanupCancelSettings getCancelSettings() {
        return cancelSettings;
    }

    public boolean isAsyncRemoval() {
        return asyncRemoval;
    }

    /**
     * Encapsulates pile detection thresholds and tracking preferences for cleanup passes.
     */
    public static final class PileDetectionSettings {

        private final int maxPerBlock;
        private final Set<EntityType> trackedTypes;
        private final boolean ignoreNamedEntities;

        private PileDetectionSettings(int maxPerBlock, Set<EntityType> trackedTypes, boolean ignoreNamedEntities) {
            this.maxPerBlock = maxPerBlock;
            this.trackedTypes = trackedTypes;
            this.ignoreNamedEntities = ignoreNamedEntities;
        }

        public int getMaxPerBlock() {
            return maxPerBlock;
        }

        public Set<EntityType> getTrackedTypes() {
            return trackedTypes;
        }

        public boolean ignoreNamedEntities() {
            return ignoreNamedEntities;
        }

        public boolean isTracking(EntityType type) {
            return trackedTypes.contains(type);
        }
    }
}
