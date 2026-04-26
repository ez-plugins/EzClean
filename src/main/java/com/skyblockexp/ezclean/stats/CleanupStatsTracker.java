package com.skyblockexp.ezclean.stats;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.Model;
import com.skyblockexp.ezclean.storage.StorageService;
import com.skyblockexp.ezclean.storage.model.CleanerTotalsModel;
import com.skyblockexp.ezclean.storage.model.CountModel;
import com.skyblockexp.ezclean.storage.model.CountModel.CountType;
import com.skyblockexp.ezclean.storage.model.LastRunModel;
import com.skyblockexp.ezclean.util.FoliaScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * Persists and aggregates cleanup run statistics for EzClean.
 *
 * <p>All writes are performed asynchronously via {@link FoliaScheduler#runAsync}.
 * Reads may be called from any thread; SQLite/MySQL reads are fast enough for
 * the command-response and scheduler paths that invoke {@link #getSnapshot}.
 */
public final class CleanupStatsTracker {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final StorageService storage;
    /** Serialises concurrent writes for the same cleaner's aggregated row. */
    private final Object writeLock = new Object();

    public CleanupStatsTracker(JavaPlugin plugin, StorageService storage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(storage, "storage");
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.storage = storage;
    }

    /**
     * No-op: Jaloquent commits writes immediately; nothing to flush on shutdown.
     */
    public void shutdown() {
        // Jaloquent writes are committed per-operation; no flush required.
    }

    /**
     * Records the result of a single cleanup run for the given cleaner.
     *
     * <p>The database update is dispatched asynchronously so as not to block
     * the server thread.
     */
    public void recordRun(String cleanerId, CleanupRunStats runStats) {
        Objects.requireNonNull(cleanerId, "cleanerId");
        Objects.requireNonNull(runStats, "runStats");
        FoliaScheduler.runAsync(plugin, () -> {
            synchronized (writeLock) {
                try {
                    persistRun(cleanerId, runStats);
                } catch (StorageException ex) {
                    logger.log(Level.WARNING, "Failed to persist cleanup run stats for '" + cleanerId + "'.", ex);
                }
            }
        });
    }

    /**
     * Returns a snapshot of accumulated statistics for the given cleaner,
     * or {@code null} if no runs have been recorded yet.
     */
    public @Nullable CleanupStatsSnapshot getSnapshot(String cleanerId) {
        Objects.requireNonNull(cleanerId, "cleanerId");
        try {
            return loadSnapshot(cleanerId);
        } catch (StorageException ex) {
            logger.log(Level.WARNING, "Failed to read cleanup stats for '" + cleanerId + "'.", ex);
            return null;
        }
    }

    /**
     * Returns snapshots for every cleaner that has recorded at least one run.
     */
    public Map<String, CleanupStatsSnapshot> getAllSnapshots() {
        try {
            List<CleanerTotalsModel> allTotals = storage.getTotalsRepo().query(Model.queryBuilder().build());
            Map<String, CleanupStatsSnapshot> snapshots = new LinkedHashMap<>();
            for (CleanerTotalsModel totalsModel : allTotals) {
                String cid = totalsModel.getId();
                CleanupStatsSnapshot snapshot = loadSnapshot(cid);
                if (snapshot != null) {
                    snapshots.put(cid, snapshot);
                }
            }
            return Collections.unmodifiableMap(snapshots);
        } catch (StorageException ex) {
            logger.log(Level.WARNING, "Failed to read all cleanup stats snapshots.", ex);
            return Collections.emptyMap();
        }
    }

    // -----------------------------------------------------------------------------------------
    // Private persistence helpers
    // -----------------------------------------------------------------------------------------

    void persistRun(String cleanerId, CleanupRunStats runStats) throws StorageException {
        // --- 1. Upsert cumulative totals row ---
        Optional<CleanerTotalsModel> existing = storage.getTotalsRepo().find(cleanerId);
        CleanerTotalsModel totals = existing.orElseGet(() -> new CleanerTotalsModel(cleanerId));

        totals.setRuns(totals.getRuns() + 1L);
        totals.setRemoved(totals.getRemoved() + runStats.removed());
        totals.setDurationMs(totals.getDurationMs() + runStats.durationMillis());
        if (runStats.tpsImpact() != null) {
            totals.setTpsImpactTotal(totals.getTpsImpactTotal() + runStats.tpsImpact());
            totals.setTpsSamples(totals.getTpsSamples() + 1L);
        }
        storage.saveModel("ezclean_stats_totals", totals);

        // --- 2. Increment total group / world count rows ---
        incrementCounts(cleanerId, CountType.TOTAL_GROUP, runStats.groupCounts());
        incrementCounts(cleanerId, CountType.TOTAL_WORLD, runStats.worldCounts());

        // --- 3. Upsert last-run row ---
        LastRunModel lastRun = new LastRunModel(cleanerId);
        lastRun.setTimestampMs(runStats.timestampMillis());
        lastRun.setRemoved(runStats.removed());
        lastRun.setDurationMs(runStats.durationMillis());
        lastRun.setTpsBefore(runStats.tpsBefore());
        lastRun.setTpsAfter(runStats.tpsAfter());
        lastRun.setTpsImpact(runStats.tpsImpact());
        storage.saveModel("ezclean_last_run", lastRun);

        // --- 4. Replace last-run group / world count rows ---
        replaceLastRunCounts(cleanerId, CountType.LAST_RUN_GROUP, runStats.groupCounts());
        replaceLastRunCounts(cleanerId, CountType.LAST_RUN_WORLD, runStats.worldCounts());
    }

    private void incrementCounts(String cleanerId, CountType type, Map<String, Integer> deltas)
            throws StorageException {
        for (Map.Entry<String, Integer> entry : deltas.entrySet()) {
            String rowId = CountModel.buildId(cleanerId, type, entry.getKey());
            Optional<CountModel> found = storage.getCountsRepo().find(rowId);
            CountModel row = found.orElseGet(() ->
                    CountModel.create(cleanerId, type, entry.getKey(), 0L));
            row.setCount(row.getCount() + entry.getValue());
            storage.saveModel("ezclean_counts", row);
        }
    }

    private void replaceLastRunCounts(String cleanerId, CountType type, Map<String, Integer> fresh)
            throws StorageException {
        // Delete stale rows for this cleaner + type, then write fresh values.
        var deleteQuery = Model.queryBuilder()
                .whereEquals("cleaner_id", cleanerId)
                .whereEquals("count_type", type.name())
                .build();
        storage.getCountsRepo().deleteWhere(deleteQuery);

        for (Map.Entry<String, Integer> entry : fresh.entrySet()) {
            CountModel row = CountModel.create(cleanerId, type, entry.getKey(), entry.getValue());
            storage.saveModel("ezclean_counts", row);
        }
    }

    private @Nullable CleanupStatsSnapshot loadSnapshot(String cleanerId) throws StorageException {
        Optional<CleanerTotalsModel> totalsOpt = storage.getTotalsRepo().find(cleanerId);
        if (totalsOpt.isEmpty()) {
            return null;
        }
        CleanerTotalsModel totalsModel = totalsOpt.get();

        Map<String, Long> groupTotals = loadCounts(cleanerId, CountType.TOTAL_GROUP);
        Map<String, Long> worldTotals = loadCounts(cleanerId, CountType.TOTAL_WORLD);

        CleanupStatsSnapshot.Totals totals = new CleanupStatsSnapshot.Totals(
                totalsModel.getRuns(),
                totalsModel.getRemoved(),
                totalsModel.getDurationMs(),
                totalsModel.getTpsImpactTotal(),
                totalsModel.getTpsSamples(),
                groupTotals,
                worldTotals);

        Optional<LastRunModel> lastRunOpt = storage.getLastRunRepo().find(cleanerId);
        CleanupStatsSnapshot.LastRun lastRun = null;
        if (lastRunOpt.isPresent()) {
            LastRunModel lr = lastRunOpt.get();
            Map<String, Long> lastRunGroups = loadCounts(cleanerId, CountType.LAST_RUN_GROUP);
            Map<String, Long> lastRunWorlds = loadCounts(cleanerId, CountType.LAST_RUN_WORLD);
            lastRun = new CleanupStatsSnapshot.LastRun(
                    lr.getTimestampMs(),
                    lr.getRemoved(),
                    lr.getDurationMs(),
                    lr.getTpsBefore(),
                    lr.getTpsAfter(),
                    lr.getTpsImpact(),
                    lastRunGroups,
                    lastRunWorlds);
        }

        return new CleanupStatsSnapshot(cleanerId, totals, lastRun);
    }

    private Map<String, Long> loadCounts(String cleanerId, CountType type) throws StorageException {
        var query = Model.queryBuilder()
                .whereEquals("cleaner_id", cleanerId)
                .whereEquals("count_type", type.name())
                .build();
        List<CountModel> rows = storage.getCountsRepo().query(query);
        Map<String, Long> result = new HashMap<>();
        for (CountModel row : rows) {
            result.put(row.getKeyName(), row.getCount());
        }
        return Collections.unmodifiableMap(result);
    }

    // -----------------------------------------------------------------------------------------
    // Nested record types (public API — unchanged)
    // -----------------------------------------------------------------------------------------

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
