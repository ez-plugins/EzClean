package com.skyblockexp.ezclean.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skyblockexp.ezclean.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable view of the EzClean configuration options for a single cleanup profile.
 *
 * <p>Construct instances via {@link #fromConfiguration(FileConfiguration, Logger)} or the
 * package-private {@link Builder}. All sub-feature groups are accessible both through
 * dedicated sub-settings objects (e.g. {@link #getBroadcastSettings()}) and through
 * backward-compatible flat getters for code that predates the split.</p>
 */
public final class CleanupSettings {

    // -----------------------------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------------------------

    private static final long TICKS_PER_MINUTE = 20L * 60L;

    private static final String DEFAULT_WARNING_MESSAGE =
            "<yellow>⚠ Entity cleanup in <gold>{minutes}</gold> minutes. Clear valuables!</yellow>";
    private static final String DEFAULT_START_MESSAGE =
            "<red><bold>✦ Entity cleanup commencing...</bold></red>";
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

    private static final Pattern MINIMESSAGE_PLACEHOLDER_PATTERN =
            Pattern.compile("\\{([a-zA-Z0-9_-]+)\\}");

    // -----------------------------------------------------------------------------------------
    // Core fields
    // -----------------------------------------------------------------------------------------

    private final String cleanerId;
    private final long cleanupIntervalTicks;
    private final long cleanupIntervalMinutes;
    private final Set<String> enabledWorlds;
    private final Set<EntityType> forcedKeeps;
    private final Set<EntityType> forcedRemovals;
    private final boolean asyncRemoval;
    private final int asyncRemovalBatchSize;
    private final SpawnReasonFilter globalSpawnReasonFilter;
    private final Map<String, SpawnReasonFilter> worldSpawnReasonFilters;
    private final int globalMinPlayers;
    private final Map<String, Integer> worldMinPlayers;
    private final Map<String, Long> worldIntervalOverrides;
    private final List<String> postCleanupCommands;

    // -----------------------------------------------------------------------------------------
    // Sub-settings objects
    // -----------------------------------------------------------------------------------------

    private final BroadcastSettings broadcastSettings;
    private final RemovalSettings removalSettings;
    private final ProtectSettings protectSettings;
    private final CleanupCancelSettings cancelSettings;
    private final TpsAwareSettings tpsAwareSettings;
    private final @Nullable PileDetectionSettings pileDetectionSettings;
    private final @Nullable ChunkCapSettings chunkCapSettings;
    private final @Nullable MergeSettings mergeSettings;
    private final EzCountdownSettings ezCountdownSettings;
    private final SparkSettings sparkSettings;

    // -----------------------------------------------------------------------------------------
    // Constructor (private, use Builder)
    // -----------------------------------------------------------------------------------------

    private CleanupSettings(Builder b) {
        this.cleanerId = b.cleanerId;
        this.cleanupIntervalTicks = b.cleanupIntervalTicks;
        this.cleanupIntervalMinutes = b.cleanupIntervalMinutes;
        this.enabledWorlds = Collections.unmodifiableSet(new HashSet<>(b.enabledWorlds));
        this.forcedKeeps = Collections.unmodifiableSet(new HashSet<>(b.forcedKeeps));
        this.forcedRemovals = Collections.unmodifiableSet(new HashSet<>(b.forcedRemovals));
        this.asyncRemoval = b.asyncRemoval;
        this.asyncRemovalBatchSize = b.asyncRemovalBatchSize;
        this.globalSpawnReasonFilter = b.globalSpawnReasonFilter;
        this.worldSpawnReasonFilters = Collections.unmodifiableMap(new HashMap<>(b.worldSpawnReasonFilters));
        this.globalMinPlayers = Math.max(0, b.globalMinPlayers);
        this.worldMinPlayers = Collections.unmodifiableMap(new HashMap<>(b.worldMinPlayers));
        this.worldIntervalOverrides = Collections.unmodifiableMap(new HashMap<>(b.worldIntervalOverrides));
        List<String> cmds = new ArrayList<>();
        for (String cmd : b.postCleanupCommands) {
            if (cmd != null && !cmd.isBlank()) {
                cmds.add(cmd);
            }
        }
        this.postCleanupCommands = Collections.unmodifiableList(cmds);
        this.broadcastSettings = b.broadcastSettings;
        this.removalSettings = b.removalSettings;
        this.protectSettings = b.protectSettings;
        this.cancelSettings = b.cancelSettings;
        this.tpsAwareSettings = b.tpsAwareSettings;
        this.pileDetectionSettings = b.pileDetectionSettings;
        this.chunkCapSettings = b.chunkCapSettings;
        this.mergeSettings = b.mergeSettings;
        this.ezCountdownSettings = b.ezCountdownSettings;
        this.sparkSettings = b.sparkSettings;
    }

    // -----------------------------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------------------------

    /**
     * Fluent builder for {@link CleanupSettings}.
     *
     * <p>All sub-settings fields default to disabled / empty instances if not set.</p>
     */
    static final class Builder {

        private final String cleanerId;
        private long cleanupIntervalTicks;
        private long cleanupIntervalMinutes;
        private Set<String> enabledWorlds = Collections.singleton("*");
        private Set<EntityType> forcedKeeps = Collections.emptySet();
        private Set<EntityType> forcedRemovals = Collections.emptySet();
        private boolean asyncRemoval;
        private int asyncRemovalBatchSize = 500;
        private SpawnReasonFilter globalSpawnReasonFilter = SpawnReasonFilter.empty();
        private Map<String, SpawnReasonFilter> worldSpawnReasonFilters = Collections.emptyMap();
        private int globalMinPlayers;
        private Map<String, Integer> worldMinPlayers = Collections.emptyMap();
        private Map<String, Long> worldIntervalOverrides = Collections.emptyMap();
        private List<String> postCleanupCommands = Collections.emptyList();
        private BroadcastSettings broadcastSettings;
        private RemovalSettings removalSettings;
        private ProtectSettings protectSettings;
        private CleanupCancelSettings cancelSettings = CleanupCancelSettings.disabled();
        private TpsAwareSettings tpsAwareSettings = TpsAwareSettings.defaults();
        private @Nullable PileDetectionSettings pileDetectionSettings;
        private @Nullable ChunkCapSettings chunkCapSettings;
        private @Nullable MergeSettings mergeSettings;
        private EzCountdownSettings ezCountdownSettings = EzCountdownSettings.disabled();
        private SparkSettings sparkSettings = SparkSettings.disabled();

        Builder(String cleanerId) {
            this.cleanerId = cleanerId;
        }

        Builder cleanupInterval(long ticks, long minutes) {
            this.cleanupIntervalTicks = ticks;
            this.cleanupIntervalMinutes = minutes;
            return this;
        }

        Builder broadcastSettings(BroadcastSettings s) {
            this.broadcastSettings = s;
            return this;
        }

        Builder removalSettings(RemovalSettings s) {
            this.removalSettings = s;
            return this;
        }

        Builder protectSettings(ProtectSettings s) {
            this.protectSettings = s;
            return this;
        }

        Builder enabledWorlds(Set<String> s) {
            this.enabledWorlds = s;
            return this;
        }

        Builder forcedKeeps(Set<EntityType> s) {
            this.forcedKeeps = s;
            return this;
        }

        Builder forcedRemovals(Set<EntityType> s) {
            this.forcedRemovals = s;
            return this;
        }

        Builder cancelSettings(CleanupCancelSettings s) {
            this.cancelSettings = s;
            return this;
        }

        Builder asyncRemoval(boolean enabled, int batchSize) {
            this.asyncRemoval = enabled;
            this.asyncRemovalBatchSize = batchSize;
            return this;
        }

        Builder globalSpawnReasonFilter(SpawnReasonFilter f) {
            this.globalSpawnReasonFilter = f;
            return this;
        }

        Builder worldSpawnReasonFilters(Map<String, SpawnReasonFilter> m) {
            this.worldSpawnReasonFilters = m;
            return this;
        }

        Builder globalMinPlayers(int v) {
            this.globalMinPlayers = v;
            return this;
        }

        Builder worldMinPlayers(Map<String, Integer> m) {
            this.worldMinPlayers = m;
            return this;
        }

        Builder worldIntervalOverrides(Map<String, Long> m) {
            this.worldIntervalOverrides = m;
            return this;
        }

        Builder postCleanupCommands(List<String> l) {
            this.postCleanupCommands = l;
            return this;
        }

        Builder tpsAwareSettings(TpsAwareSettings s) {
            this.tpsAwareSettings = s;
            return this;
        }

        Builder pileDetectionSettings(@Nullable PileDetectionSettings s) {
            this.pileDetectionSettings = s;
            return this;
        }

        Builder chunkCapSettings(@Nullable ChunkCapSettings s) {
            this.chunkCapSettings = s;
            return this;
        }

        Builder mergeSettings(@Nullable MergeSettings s) {
            this.mergeSettings = s;
            return this;
        }

        Builder ezCountdownSettings(EzCountdownSettings s) {
            this.ezCountdownSettings = s;
            return this;
        }

        Builder sparkSettings(SparkSettings s) {
            this.sparkSettings = s;
            return this;
        }

        CleanupSettings build() {
            if (broadcastSettings == null) {
                throw new IllegalStateException(
                        "broadcastSettings must be set before building CleanupSettings for '" + cleanerId + "'");
            }
            if (removalSettings == null) {
                throw new IllegalStateException(
                        "removalSettings must be set before building CleanupSettings for '" + cleanerId + "'");
            }
            if (protectSettings == null) {
                throw new IllegalStateException(
                        "protectSettings must be set before building CleanupSettings for '" + cleanerId + "'");
            }
            return new CleanupSettings(this);
        }
    }

    // -----------------------------------------------------------------------------------------
    // Public factory methods
    // -----------------------------------------------------------------------------------------

    /**
     * Loads a list of cleanup profiles from the assembled (multi-file) configuration.
     *
     * @param config the combined configuration to read from
     * @param logger the plugin logger used to report invalid entries
     * @return an immutable list of settings instances (at least one)
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

    // -----------------------------------------------------------------------------------------
    // Sub-settings accessors
    // -----------------------------------------------------------------------------------------

    /** All broadcast / warning / reminder settings for this profile. */
    public BroadcastSettings getBroadcastSettings() {
        return broadcastSettings;
    }

    /** All entity removal category flags and age / proximity gates. */
    public RemovalSettings getRemovalSettings() {
        return removalSettings;
    }

    /** All entity protection flags, name-tag patterns, and item-metadata filter. */
    public ProtectSettings getProtectSettings() {
        return protectSettings;
    }

    /** Cancel-mechanic settings (cost, messages, enabled state). */
    public CleanupCancelSettings getCancelSettings() {
        return cancelSettings;
    }

    /** TPS/MSPT-aware deferral settings. */
    public TpsAwareSettings getTpsAwareSettings() {
        return tpsAwareSettings;
    }

    /** Pile-detection settings, or {@code null} when pile detection is disabled. */
    public @Nullable PileDetectionSettings getPileDetectionSettings() {
        return pileDetectionSettings;
    }

    /** Per-chunk entity cap settings, or {@code null} when chunk caps are disabled. */
    public @Nullable ChunkCapSettings getChunkCapSettings() {
        return chunkCapSettings;
    }

    /** Entity/item merging settings, or {@code null} when merging is disabled. */
    public @Nullable MergeSettings getMergeSettings() {
        return mergeSettings;
    }

    /** EzCountdown integration settings. */

    public EzCountdownSettings getEzCountdownSettings() {
        return ezCountdownSettings;
    }

    /** Spark profiler integration settings. */
    public SparkSettings getSparkSettings() {
        return sparkSettings;
    }

    // -----------------------------------------------------------------------------------------
    // Core getters
    // -----------------------------------------------------------------------------------------

    public String getCleanerId() {
        return cleanerId;
    }

    public long getCleanupIntervalTicks() {
        return cleanupIntervalTicks;
    }

    public long getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }

    public boolean isWorldEnabled(String worldName) {
        return enabledWorlds.contains("*")
                || enabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
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

    public boolean isAsyncRemoval() {
        return asyncRemoval;
    }

    public int getAsyncRemovalBatchSize() {
        return asyncRemovalBatchSize;
    }

    /**
     * Returns the spawn-reason filter for the given world, falling back to the global
     * (profile-level) filter when no world-specific override is configured.
     */
    public SpawnReasonFilter getSpawnReasonFilter(String worldName) {
        SpawnReasonFilter worldFilter =
                worldSpawnReasonFilters.get(worldName.toLowerCase(Locale.ROOT));
        return worldFilter != null ? worldFilter : globalSpawnReasonFilter;
    }

    public int getGlobalMinPlayers() {
        return globalMinPlayers;
    }

    public int getWorldMinPlayers(String worldName) {
        return worldMinPlayers.getOrDefault(worldName.toLowerCase(Locale.ROOT), 0);
    }

    public Map<String, Long> getWorldIntervalOverrides() {
        return worldIntervalOverrides;
    }

    public List<String> getPostCleanupCommands() {
        return postCleanupCommands;
    }

    public boolean isPileDetectionEnabled() {
        return pileDetectionSettings != null;
    }

    public boolean isChunkCapEnabled() {
        return chunkCapSettings != null;
    }

    public boolean isMergingEnabled() {
        return mergeSettings != null;
    }

    // -----------------------------------------------------------------------------------------
    // Flat delegating getters — broadcast (backward-compatible API)
    // -----------------------------------------------------------------------------------------

    public boolean isWarningEnabled() { return broadcastSettings.isWarningEnabled(); }
    public long getWarningOffsetTicks() { return broadcastSettings.getWarningOffsetTicks(); }
    public long getWarningMinutesBefore() { return broadcastSettings.getWarningMinutesBefore(); }
    public String getWarningMessageTemplate() { return broadcastSettings.getWarningMessageTemplate(); }

    public boolean isStartBroadcastEnabled() { return broadcastSettings.isStartBroadcastEnabled(); }
    public String getStartMessageTemplate() { return broadcastSettings.getStartMessageTemplate(); }

    public boolean isSummaryBroadcastEnabled() { return broadcastSettings.isSummaryBroadcastEnabled(); }
    public String getSummaryMessageTemplate() { return broadcastSettings.getSummaryMessageTemplate(); }
    public @Nullable String getPreCleanMessageTemplate() { return broadcastSettings.getPreCleanMessageTemplate(); }

    public boolean isIntervalBroadcastEnabled() { return broadcastSettings.isIntervalBroadcastEnabled(); }
    public long getIntervalBroadcastMinutes() { return broadcastSettings.getIntervalBroadcastMinutes(); }
    public String getIntervalBroadcastMessageTemplate() { return broadcastSettings.getIntervalBroadcastMessageTemplate(); }

    public boolean isDynamicBroadcastEnabled() { return broadcastSettings.isDynamicBroadcastEnabled(); }
    public Set<Long> getDynamicBroadcastMinutes() { return broadcastSettings.getDynamicBroadcastMinutes(); }
    public Set<Long> getDynamicBroadcastSeconds() { return broadcastSettings.getDynamicBroadcastSeconds(); }
    public String getDynamicBroadcastMessageTemplate() { return broadcastSettings.getDynamicBroadcastMessageTemplate(); }

    public boolean isStatsSummaryBroadcastEnabled() { return broadcastSettings.isStatsSummaryBroadcastEnabled(); }
    public long getStatsSummaryEveryRuns() { return broadcastSettings.getStatsSummaryEveryRuns(); }
    public String getStatsSummaryMessageTemplate() { return broadcastSettings.getStatsSummaryMessageTemplate(); }

    // -----------------------------------------------------------------------------------------
    // Flat delegating getters — removal (backward-compatible API)
    // -----------------------------------------------------------------------------------------

    public boolean removeHostileMobs() { return removalSettings.removeHostileMobs(); }
    public boolean removePassiveMobs() { return removalSettings.removePassiveMobs(); }
    public boolean removeVillagers() { return removalSettings.removeVillagers(); }
    public boolean removeVehicles() { return removalSettings.removeVehicles(); }
    public boolean removeDroppedItems() { return removalSettings.removeDroppedItems(); }
    public boolean removeProjectiles() { return removalSettings.removeProjectiles(); }
    public boolean removeExperienceOrbs() { return removalSettings.removeExperienceOrbs(); }
    public boolean removeAreaEffectClouds() { return removalSettings.removeAreaEffectClouds(); }
    public boolean removeFallingBlocks() { return removalSettings.removeFallingBlocks(); }
    public boolean removePrimedTnt() { return removalSettings.removePrimedTnt(); }
    public int getDroppedItemsMinAgeTicks() { return removalSettings.getDroppedItemsMinAgeTicks(); }
    public int getExperienceOrbsMinAgeTicks() { return removalSettings.getExperienceOrbsMinAgeTicks(); }
    public int getItemPlayerProximityBlocks() { return removalSettings.getItemPlayerProximityBlocks(); }
    public int getOrbPlayerProximityBlocks() { return removalSettings.getOrbPlayerProximityBlocks(); }

    // -----------------------------------------------------------------------------------------
    // Flat delegating getters — protection (backward-compatible API)
    // -----------------------------------------------------------------------------------------

    public boolean protectPlayers() { return protectSettings.protectPlayers(); }
    public boolean protectArmorStands() { return protectSettings.protectArmorStands(); }
    public boolean protectDisplayEntities() { return protectSettings.protectDisplayEntities(); }
    public boolean protectTamedMobs() { return protectSettings.protectTamedMobs(); }
    public boolean protectNameTaggedMobs() { return protectSettings.protectNameTaggedMobs(); }
    public List<Pattern> getNameTagPatterns() { return protectSettings.getNameTagPatterns(); }
    public ItemMetadataFilter getItemMetadataFilter() { return protectSettings.getItemMetadataFilter(); }

    // -----------------------------------------------------------------------------------------
    // Configuration loading (private)
    // -----------------------------------------------------------------------------------------

    private static CleanupSettings loadLegacySettings(
            FileConfiguration config, Logger logger, MessageConfiguration messages) {
        ConfigurationSection cleanupSection = config.getConfigurationSection("cleanup");
        if (cleanupSection == null) {
            cleanupSection = new MemoryConfiguration();
        }
        return loadFromSection("default", cleanupSection, logger, messages);
    }

    private static CleanupSettings loadFromSection(String cleanerId, ConfigurationSection section,
            Logger logger, MessageConfiguration messages) {
        String sectionPath = section.getCurrentPath();
        if (sectionPath == null || sectionPath.isBlank()) {
            sectionPath = "cleanup";
        }

        long intervalMinutes = Math.max(1L, section.getLong("interval-minutes", 60L));

        BroadcastSettings broadcast = loadBroadcastSettings(cleanerId, section, messages, intervalMinutes);
        RemovalSettings removal = RemovalSettings.load(section);
        ProtectSettings protect = ProtectSettings.load(section, logger, sectionPath);

        Set<String> worlds = parseWorlds(section.getStringList("worlds"));
        Set<EntityType> keep = parseEntityTypes(
                section.getStringList("entity-types.keep"), logger, sectionPath + ".entity-types.keep");
        Set<EntityType> remove = parseEntityTypes(
                section.getStringList("entity-types.remove"), logger, sectionPath + ".entity-types.remove");

        PileDetectionSettings pileDetection = PileDetectionSettings.load(section, logger, sectionPath);
        ChunkCapSettings chunkCaps = ChunkCapSettings.load(section, logger, sectionPath);
        MergeSettings mergeSettings = MergeSettings.load(section);

        boolean asyncRemoval = section.getBoolean("performance.async-removal", false);
        int asyncBatchSize = Math.max(1, section.getInt("performance.async-removal-batch-size", 500));

        CleanupCancelSettings cancelSettings = loadCancelSettings(cleanerId, section, messages);

        SpawnReasonFilter globalFilter = SpawnReasonFilter.parse(
                section.getConfigurationSection("spawn-reasons"), logger, sectionPath + ".spawn-reasons");

        int globalMinPlayers = Math.max(0, section.getInt("min-players", 0));
        Map<String, SpawnReasonFilter> worldFilters = new HashMap<>();
        Map<String, Integer> worldMinPlayersMap = new HashMap<>();
        Map<String, Long> worldIntervalMap = new HashMap<>();
        loadWorldOverrides(section, logger, sectionPath, worldFilters, worldMinPlayersMap, worldIntervalMap);

        List<String> postCleanupCommands = section.getStringList("post-cleanup-commands");
        TpsAwareSettings tpsAware = TpsAwareSettings.load(section);
        EzCountdownSettings ezCountdown = EzCountdownSettings.load(section);
        SparkSettings spark = SparkSettings.load(section);

        warnDangerousSettings(cleanerId, removal, protect, asyncRemoval, asyncBatchSize, remove, logger);

        return new Builder(cleanerId)
                .cleanupInterval(intervalMinutes * TICKS_PER_MINUTE, intervalMinutes)
                .broadcastSettings(broadcast)
                .removalSettings(removal)
                .protectSettings(protect)
                .enabledWorlds(worlds)
                .forcedKeeps(keep)
                .forcedRemovals(remove)
                .cancelSettings(cancelSettings)
                .asyncRemoval(asyncRemoval, asyncBatchSize)
                .globalSpawnReasonFilter(globalFilter)
                .worldSpawnReasonFilters(worldFilters.isEmpty() ? Collections.emptyMap() : worldFilters)
                .globalMinPlayers(globalMinPlayers)
                .worldMinPlayers(worldMinPlayersMap)
                .worldIntervalOverrides(worldIntervalMap)
                .postCleanupCommands(postCleanupCommands)
                .tpsAwareSettings(tpsAware)
                .pileDetectionSettings(pileDetection)
                .chunkCapSettings(chunkCaps)
                .mergeSettings(mergeSettings)
                .ezCountdownSettings(ezCountdown)
                .sparkSettings(spark)
                .build();
    }

    // -----------------------------------------------------------------------------------------
    // Sub-section loaders
    // -----------------------------------------------------------------------------------------

    private static BroadcastSettings loadBroadcastSettings(String cleanerId,
            ConfigurationSection section, MessageConfiguration messages, long intervalMinutes) {
        long warningMinutes = Math.max(0L, section.getLong("warning.minutes-before", 5L));
        boolean warningEnabled = section.getBoolean("warning.enabled", warningMinutes > 0L);
        String warningMessage = resolveMessage(cleanerId, section, messages,
                "warning.message", DEFAULT_WARNING_MESSAGE);

        boolean startEnabled = section.getBoolean("broadcast.start.enabled", true);
        String startMessage = resolveMessage(cleanerId, section, messages,
                "broadcast.start.message", DEFAULT_START_MESSAGE);

        boolean summaryEnabled = section.getBoolean("broadcast.summary.enabled", true);
        String summaryMessage = resolveMessage(cleanerId, section, messages,
                "broadcast.summary.message", DEFAULT_SUMMARY_MESSAGE);
        String preCleanMessage = resolveOptionalMessage(cleanerId, section, messages,
                "broadcast.pre-clean.message");

        ConfigurationSection intervalSection = section.getConfigurationSection("broadcast.interval");
        boolean intervalEnabled = false;
        long intervalMinutesBetween = 0L;
        if (intervalSection != null) {
            intervalEnabled = intervalSection.getBoolean("enabled", false);
            intervalMinutesBetween = Math.max(1L, intervalSection.getLong("every-minutes", 15L));
            if (intervalMinutesBetween <= 0L) {
                intervalEnabled = false;
            }
        }
        String intervalMessage = resolveMessage(cleanerId, section, messages,
                "broadcast.interval.message", DEFAULT_INTERVAL_MESSAGE);

        ConfigurationSection dynamicSection = section.getConfigurationSection("broadcast.dynamic");
        boolean dynamicEnabled = false;
        Set<Long> dynamicMinutes = Collections.emptySet();
        Set<Long> dynamicSeconds = Collections.emptySet();
        if (dynamicSection != null) {
            dynamicEnabled = dynamicSection.getBoolean("enabled", false);
            Set<Long> parsedMinutes = parseLongSet(dynamicSection.getIntegerList("minutes"));
            if (!parsedMinutes.isEmpty()) {
                dynamicMinutes = Collections.unmodifiableSet(parsedMinutes);
            } else {
                dynamicEnabled = false;
            }
            Set<Long> parsedSeconds = parseLongSet(dynamicSection.getIntegerList("seconds"));
            if (!parsedSeconds.isEmpty()) {
                dynamicSeconds = Collections.unmodifiableSet(parsedSeconds);
            }
        }
        String dynamicMessage = resolveMessage(cleanerId, section, messages,
                "broadcast.dynamic.message", DEFAULT_DYNAMIC_MESSAGE);

        ConfigurationSection statsSection = section.getConfigurationSection("broadcast.stats-summary");
        boolean statsEnabled = false;
        long statsEveryRuns = 0L;
        if (statsSection != null) {
            statsEnabled = statsSection.getBoolean("enabled", false);
            statsEveryRuns = Math.max(1L, statsSection.getLong("every-runs", 5L));
            if (statsEveryRuns <= 0L) {
                statsEnabled = false;
            }
        }
        String statsMessage = resolveMessage(cleanerId, section, messages,
                "stats-summary.message", DEFAULT_STATS_SUMMARY_MESSAGE);

        return new BroadcastSettings(
                warningEnabled, warningMinutes * TICKS_PER_MINUTE, warningMinutes, warningMessage,
                startEnabled, startMessage,
                summaryEnabled, summaryMessage, preCleanMessage,
                intervalEnabled, intervalMinutesBetween, intervalMessage,
                dynamicEnabled, dynamicMinutes, dynamicSeconds, dynamicMessage,
                statsEnabled, statsEveryRuns, statsMessage);
    }

    private static CleanupCancelSettings loadCancelSettings(String cleanerId,
            ConfigurationSection section, MessageConfiguration messages) {
        ConfigurationSection cancelSection = section.getConfigurationSection("cancel");
        if (cancelSection == null) {
            return CleanupCancelSettings.disabled();
        }
        boolean cancelEnabled = cancelSection.getBoolean("enabled", true);
        double cancelCost = cancelSection.getDouble("cost", 0.0D);
        String hoverMsg = resolveMessage(cleanerId, section, messages,
                "cancel.hover-message", DEFAULT_CANCEL_HOVER_MESSAGE);
        String successMsg = resolveMessage(cleanerId, section, messages,
                "cancel.success-message", DEFAULT_CANCEL_SUCCESS_MESSAGE);
        String broadcastMsg = resolveMessage(cleanerId, section, messages,
                "cancel.broadcast-message", DEFAULT_CANCEL_BROADCAST_MESSAGE);
        String insufficientMsg = resolveMessage(cleanerId, section, messages,
                "cancel.insufficient-funds-message", DEFAULT_CANCEL_INSUFFICIENT_FUNDS_MESSAGE);
        String disabledMsg = resolveMessage(cleanerId, section, messages,
                "cancel.disabled-message", DEFAULT_CANCEL_DISABLED_MESSAGE);
        String noEconomyMsg = resolveMessage(cleanerId, section, messages,
                "cancel.no-economy-message", DEFAULT_CANCEL_NO_ECONOMY_MESSAGE);
        return CleanupCancelSettings.create(cancelEnabled, cancelCost, hoverMsg, successMsg,
                broadcastMsg, insufficientMsg, disabledMsg, noEconomyMsg);
    }

    private static void loadWorldOverrides(ConfigurationSection section, Logger logger,
            String sectionPath, Map<String, SpawnReasonFilter> worldFilters,
            Map<String, Integer> worldMinPlayersMap, Map<String, Long> worldIntervalMap) {
        ConfigurationSection overrides = section.getConfigurationSection("world-overrides");
        if (overrides == null) {
            return;
        }
        for (String worldName : overrides.getKeys(false)) {
            ConfigurationSection worldSection = overrides.getConfigurationSection(worldName);
            if (worldSection == null) {
                continue;
            }
            SpawnReasonFilter worldFilter = SpawnReasonFilter.parse(
                    worldSection.getConfigurationSection("spawn-reasons"), logger,
                    sectionPath + ".world-overrides." + worldName + ".spawn-reasons");
            worldFilters.put(worldName.toLowerCase(Locale.ROOT), worldFilter);
            int wMin = worldSection.getInt("min-players", 0);
            if (wMin > 0) {
                worldMinPlayersMap.put(worldName.toLowerCase(Locale.ROOT), wMin);
            }
            long wInterval = worldSection.getLong("interval-minutes", -1L);
            if (wInterval >= 1L) {
                worldIntervalMap.put(worldName.toLowerCase(Locale.ROOT), wInterval);
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Danger warnings
    // -----------------------------------------------------------------------------------------

    private static void warnDangerousSettings(String cleanerId, RemovalSettings removal,
            ProtectSettings protect, boolean asyncRemoval, int asyncBatchSize,
            Set<EntityType> forcedRemovals, @Nullable Logger logger) {
        if (logger == null) {
            return;
        }
        if (removal.removePassiveMobs() && !protect.protectTamedMobs()) {
            logger.warning(String.format(
                    "[EzClean][%s] DANGEROUS CONFIG: remove.passive-mobs is true and "
                            + "protect.tamed-mobs is false — tamed pets WILL be removed during cleanups!",
                    cleanerId));
        }
        if (removal.removeVillagers()) {
            logger.warning(String.format(
                    "[EzClean][%s] WARNING: remove.villagers is true — villagers and wandering traders "
                            + "will be removed. Ensure this is intentional (e.g. not on a survival or economy server).",
                    cleanerId));
        }
        if (asyncRemoval && asyncBatchSize > 1000) {
            logger.warning(String.format(
                    "[EzClean][%s] WARNING: performance.async-removal-batch-size is very high (%d). "
                            + "Consider reducing this to avoid hitches on large servers.",
                    cleanerId, asyncBatchSize));
        }
        Set<String> bossNames = Set.of("WITHER", "ENDER_DRAGON", "WARDEN", "ELDER_GUARDIAN");
        for (EntityType type : forcedRemovals) {
            if (bossNames.contains(type.name())) {
                logger.warning(String.format(
                        "[EzClean][%s] WARNING: entity-types.remove contains '%s', which is a boss "
                                + "entity protected by the defensive blacklist. This override is intentional "
                                + "but may surprise players.",
                        cleanerId, type.name()));
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Parse utilities
    // -----------------------------------------------------------------------------------------

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
        return results.isEmpty() ? Collections.singleton("*") : Collections.unmodifiableSet(results);
    }

    private static Set<EntityType> parseEntityTypes(
            List<String> entries, Logger logger, String path) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptySet();
        }
        Set<EntityType> result = new HashSet<>();
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                result.add(EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                if (logger != null) {
                    logger.warning(String.format("Unknown entity type '%s' at '%s'; skipping.", raw, path));
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<Long> parseLongSet(List<Integer> values) {
        Set<Long> result = new LinkedHashSet<>();
        for (Integer value : values) {
            if (value != null && value >= 0) {
                result.add(value.longValue());
            }
        }
        return result;
    }

    private static String resolveMessage(String cleanerId, ConfigurationSection section,
            MessageConfiguration messages, String path, String defaultValue) {
        String value = section.getString(path);
        if (value == null || value.isEmpty()) {
            value = messages.getMessage(cleanerId, path);
        }
        if (value == null || value.isEmpty()) {
            value = Registry.getLang().get("defaults." + path);
        }
        if (value == null || value.isEmpty()) {
            value = defaultValue;
        }
        return normalizeMiniMessagePlaceholders(value);
    }

    private static @Nullable String resolveOptionalMessage(String cleanerId,
            ConfigurationSection section, MessageConfiguration messages, String path) {
        String value = section.getString(path);
        if (value == null || value.isEmpty()) {
            value = messages.getMessage(cleanerId, path);
        }
        if (value == null || value.isEmpty()) {
            value = Registry.getLang().get("defaults." + path);
        }
        return (value == null || value.isEmpty()) ? null : normalizeMiniMessagePlaceholders(value);
    }

    private static String normalizeMiniMessagePlaceholders(String template) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher matcher = MINIMESSAGE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement("<" + matcher.group(1) + ">"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    // -----------------------------------------------------------------------------------------
    // MessageConfiguration (private loading helper)
    // -----------------------------------------------------------------------------------------

    private static final class MessageConfiguration {

        private final @Nullable ConfigurationSection defaultsSection;
        private final @Nullable ConfigurationSection cleanersSection;

        private MessageConfiguration(@Nullable ConfigurationSection defaultsSection,
                @Nullable ConfigurationSection cleanersSection) {
            this.defaultsSection = defaultsSection;
            this.cleanersSection = cleanersSection;
        }

        static MessageConfiguration from(@Nullable ConfigurationSection messagesSection) {
            if (messagesSection == null) {
                return new MessageConfiguration(null, null);
            }
            return new MessageConfiguration(
                    messagesSection.getConfigurationSection("defaults"),
                    messagesSection.getConfigurationSection("cleaners"));
        }

        @Nullable String getMessage(String cleanerId, String path) {
            if (cleanersSection != null) {
                ConfigurationSection cleaner = cleanersSection.getConfigurationSection(cleanerId);
                if (cleaner != null) {
                    String value = cleaner.getString(path);
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                }
            }
            if (defaultsSection != null) {
                String value = defaultsSection.getString(path);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
            return null;
        }
    }
}
