package com.skyblockexp.ezclean.stats;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.skyblockexp.ezclean.util.FoliaScheduler;
import org.jetbrains.annotations.Nullable;

/**
 * Persists and aggregates cleanup run statistics for EzClean.
 */
public final class CleanupStatsTracker {

    private static final String ROOT_SECTION = "cleaners";

    private final File statsFile;
    private final Logger logger;
    private final JavaPlugin plugin;
    private final Object lock = new Object();
    private final AtomicBoolean pendingSave = new AtomicBoolean(false);
    private YamlConfiguration configuration;

    public CleanupStatsTracker(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "cleanup-stats.yml");
        this.logger = plugin.getLogger();
        this.configuration = loadConfiguration();
    }

    public void shutdown() {
        synchronized (lock) {
            saveConfiguration();
        }
    }

    public void recordRun(String cleanerId, CleanupRunStats runStats) {
        Objects.requireNonNull(cleanerId, "cleanerId");
        Objects.requireNonNull(runStats, "runStats");
        synchronized (lock) {
            ConfigurationSection cleanerSection = getCleanerSection(cleanerId);
            ConfigurationSection totalsSection = getOrCreateSection(cleanerSection, "totals");
            long runs = totalsSection.getLong("runs", 0L) + 1L;
            totalsSection.set("runs", runs);

            long removedTotal = totalsSection.getLong("removed", 0L) + runStats.removed();
            totalsSection.set("removed", removedTotal);

            long durationTotal = totalsSection.getLong("duration-ms", 0L) + runStats.durationMillis();
            totalsSection.set("duration-ms", durationTotal);

            if (runStats.tpsImpact() != null) {
                double impactTotal = totalsSection.getDouble("tps-impact-total", 0.0) + runStats.tpsImpact();
                long samples = totalsSection.getLong("tps-samples", 0L) + 1L;
                totalsSection.set("tps-impact-total", impactTotal);
                totalsSection.set("tps-samples", samples);
            }

            incrementCounts(getOrCreateSection(totalsSection, "groups"), runStats.groupCounts());
            incrementCounts(getOrCreateSection(totalsSection, "worlds"), runStats.worldCounts());

            ConfigurationSection lastRunSection = getOrCreateSection(cleanerSection, "last-run");
            lastRunSection.set("timestamp", runStats.timestampMillis());
            lastRunSection.set("removed", runStats.removed());
            lastRunSection.set("duration-ms", runStats.durationMillis());
            if (runStats.tpsBefore() != null) {
                lastRunSection.set("tps-before", runStats.tpsBefore());
            } else {
                lastRunSection.set("tps-before", null);
            }
            if (runStats.tpsAfter() != null) {
                lastRunSection.set("tps-after", runStats.tpsAfter());
            } else {
                lastRunSection.set("tps-after", null);
            }
            if (runStats.tpsImpact() != null) {
                lastRunSection.set("tps-impact", runStats.tpsImpact());
            } else {
                lastRunSection.set("tps-impact", null);
            }

            replaceCounts(lastRunSection, "groups", runStats.groupCounts());
            replaceCounts(lastRunSection, "worlds", runStats.worldCounts());
        }
        scheduleAsyncSave();
    }

    private void scheduleAsyncSave() {
        if (pendingSave.compareAndSet(false, true)) {
            FoliaScheduler.runAsync(plugin, () -> {
                synchronized (lock) {
                    saveConfiguration();
                }
                pendingSave.set(false);
            });
        }
    }

    public @Nullable CleanupStatsSnapshot getSnapshot(String cleanerId) {
        Objects.requireNonNull(cleanerId, "cleanerId");
        synchronized (lock) {
            ConfigurationSection cleanerSection = configuration.getConfigurationSection(ROOT_SECTION + "." + cleanerId);
            if (cleanerSection == null) {
                return null;
            }

            ConfigurationSection totalsSection = cleanerSection.getConfigurationSection("totals");
            CleanupStatsSnapshot.Totals totals = totalsSection == null
                    ? CleanupStatsSnapshot.Totals.empty()
                    : new CleanupStatsSnapshot.Totals(
                            totalsSection.getLong("runs", 0L),
                            totalsSection.getLong("removed", 0L),
                            totalsSection.getLong("duration-ms", 0L),
                            totalsSection.getDouble("tps-impact-total", 0.0),
                            totalsSection.getLong("tps-samples", 0L),
                            readCounts(totalsSection.getConfigurationSection("groups")),
                            readCounts(totalsSection.getConfigurationSection("worlds"))
                    );

            ConfigurationSection lastRunSection = cleanerSection.getConfigurationSection("last-run");
            CleanupStatsSnapshot.LastRun lastRun = null;
            if (lastRunSection != null && lastRunSection.getLong("timestamp", 0L) > 0L) {
                Double tpsBefore = lastRunSection.contains("tps-before")
                        ? lastRunSection.getDouble("tps-before")
                        : null;
                Double tpsAfter = lastRunSection.contains("tps-after")
                        ? lastRunSection.getDouble("tps-after")
                        : null;
                Double tpsImpact = lastRunSection.contains("tps-impact")
                        ? lastRunSection.getDouble("tps-impact")
                        : null;
                lastRun = new CleanupStatsSnapshot.LastRun(
                        lastRunSection.getLong("timestamp", 0L),
                        lastRunSection.getLong("removed", 0L),
                        lastRunSection.getLong("duration-ms", 0L),
                        tpsBefore,
                        tpsAfter,
                        tpsImpact,
                        readCounts(lastRunSection.getConfigurationSection("groups")),
                        readCounts(lastRunSection.getConfigurationSection("worlds"))
                );
            }

            return new CleanupStatsSnapshot(cleanerId, totals, lastRun);
        }
    }

    public Map<String, CleanupStatsSnapshot> getAllSnapshots() {
        synchronized (lock) {
            ConfigurationSection root = configuration.getConfigurationSection(ROOT_SECTION);
            if (root == null) {
                return Collections.emptyMap();
            }
            Map<String, CleanupStatsSnapshot> snapshots = new LinkedHashMap<>();
            for (String cleanerId : root.getKeys(false)) {
                CleanupStatsSnapshot snapshot = getSnapshot(cleanerId);
                if (snapshot != null) {
                    snapshots.put(cleanerId, snapshot);
                }
            }
            return Collections.unmodifiableMap(snapshots);
        }
    }

    private YamlConfiguration loadConfiguration() {
        if (!statsFile.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(statsFile);
    }

    private void saveConfiguration() {
        try {
            configuration.save(statsFile);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save EzClean cleanup-stats.yml.", ex);
        }
    }

    private ConfigurationSection getCleanerSection(String cleanerId) {
        ConfigurationSection root = getOrCreateSection(configuration, ROOT_SECTION);
        return getOrCreateSection(root, cleanerId);
    }

    private static ConfigurationSection getOrCreateSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section != null) {
            return section;
        }
        return parent.createSection(path);
    }

    private static void incrementCounts(ConfigurationSection section, Map<String, Integer> values) {
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            String key = entry.getKey();
            long current = section.getLong(key, 0L);
            section.set(key, current + entry.getValue());
        }
    }

    private static void replaceCounts(ConfigurationSection parent, String childName, Map<String, Integer> values) {
        parent.set(childName, null);
        ConfigurationSection section = parent.createSection(childName);
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            section.set(entry.getKey(), entry.getValue());
        }
    }

    private static Map<String, Long> readCounts(@Nullable ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            counts.put(key, section.getLong(key, 0L));
        }
        return Collections.unmodifiableMap(counts);
    }

    public record CleanupRunStats(long timestampMillis, long removed, long durationMillis,
            @Nullable Double tpsBefore, @Nullable Double tpsAfter, @Nullable Double tpsImpact,
            Map<String, Integer> groupCounts, Map<String, Integer> worldCounts) {

        public CleanupRunStats {
            Objects.requireNonNull(groupCounts, "groupCounts");
            Objects.requireNonNull(worldCounts, "worldCounts");
        }

        public static CleanupRunStats create(long removed, long durationMillis,
                @Nullable Double tpsBefore, @Nullable Double tpsAfter, @Nullable Double tpsImpact,
                Map<String, Integer> groupCounts, Map<String, Integer> worldCounts) {
            return new CleanupRunStats(Instant.now().toEpochMilli(), removed, durationMillis,
                    tpsBefore, tpsAfter, tpsImpact, groupCounts, worldCounts);
        }
    }

    public record CleanupStatsSnapshot(String cleanerId, Totals totals, @Nullable LastRun lastRun) {

        public record Totals(long runs, long removed, long durationMillis,
                double tpsImpactTotal, long tpsSamples,
                Map<String, Long> groupTotals, Map<String, Long> worldTotals) {

            static Totals empty() {
                return new Totals(0L, 0L, 0L, 0.0, 0L, Collections.emptyMap(), Collections.emptyMap());
            }

            public @Nullable Double averageTpsImpact() {
                if (tpsSamples <= 0L) {
                    return null;
                }
                return tpsImpactTotal / tpsSamples;
            }

            public @Nullable Long averageDurationMillis() {
                if (runs <= 0L) {
                    return null;
                }
                return durationMillis / runs;
            }
        }

        public record LastRun(long timestampMillis, long removed, long durationMillis,
                @Nullable Double tpsBefore, @Nullable Double tpsAfter, @Nullable Double tpsImpact,
                Map<String, Long> groupCounts, Map<String, Long> worldCounts) {
        }
    }
}
