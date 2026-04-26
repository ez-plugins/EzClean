package com.skyblockexp.ezclean.stats;

import com.skyblockexp.ezclean.stats.CleanupStatsTracker.CleanupRunStats;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker.CleanupStatsSnapshot;
import com.skyblockexp.ezclean.storage.StorageService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify {@link CleanupStatsTracker} actually persists and
 * retrieves data as expected, using a real SQLite database written to a temporary
 * directory (one clean database per test method).
 *
 * <p>{@code persistRun} is called directly (it is package-private) so the tests
 * exercise the full storage round-trip without the async dispatch of
 * {@link CleanupStatsTracker#recordRun}, which is a separate concern.
 */
class CleanupStatsTrackerStorageTest {

    @TempDir
    Path tempDir;

    private CleanupStatsTracker tracker;

    @BeforeEach
    void setUp() throws Exception {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        // Empty config → defaults to SQLite; DB file placed inside tempDir.
        StorageService storageService = StorageService.create(
                new YamlConfiguration(),
                tempDir.toFile(),
                Logger.getLogger("test"));

        tracker = new CleanupStatsTracker(plugin, storageService);
    }

    // -----------------------------------------------------------------------------------------
    // Empty-state behaviour
    // -----------------------------------------------------------------------------------------

    @Test
    void getSnapshot_returnsNull_whenNoRunRecorded() {
        assertNull(tracker.getSnapshot("nonexistent-cleaner"));
    }

    @Test
    void getAllSnapshots_returnsEmptyMap_whenNoRunsRecorded() {
        assertTrue(tracker.getAllSnapshots().isEmpty());
    }

    // -----------------------------------------------------------------------------------------
    // Single-run persistence
    // -----------------------------------------------------------------------------------------

    @Test
    void singleRun_totals_arePersistedCorrectly() throws Exception {
        CleanupRunStats run = new CleanupRunStats(
                1_000L, 42L, 500L,
                19.5, 18.2, -1.3,
                Map.of("group1", 10, "group2", 5),
                Map.of("overworld", 15));

        tracker.persistRun("cleaner-a", run);

        CleanupStatsSnapshot snapshot = tracker.getSnapshot("cleaner-a");
        assertNotNull(snapshot, "snapshot must exist after recording a run");
        assertEquals("cleaner-a", snapshot.cleanerId());

        CleanupStatsSnapshot.Totals totals = snapshot.totals();
        assertEquals(1L,    totals.runs(),           "runs");
        assertEquals(42L,   totals.removed(),        "removed");
        assertEquals(500L,  totals.durationMillis(), "durationMillis");
        assertEquals(-1.3,  totals.tpsImpactTotal(), 1e-9, "tpsImpactTotal");
        assertEquals(1L,    totals.tpsSamples(),     "tpsSamples");
        assertEquals(Map.of("group1", 10L, "group2", 5L), totals.groupTotals(),  "groupTotals");
        assertEquals(Map.of("overworld", 15L),            totals.worldTotals(),  "worldTotals");
    }

    @Test
    void singleRun_lastRun_isPersistedCorrectly() throws Exception {
        CleanupRunStats run = new CleanupRunStats(
                1_234_567_890L, 7L, 200L,
                19.8, 19.0, -0.8,
                Map.of("heroes", 3),
                Map.of("nether", 7));

        tracker.persistRun("cleaner-b", run);

        CleanupStatsSnapshot.LastRun lastRun = tracker.getSnapshot("cleaner-b").lastRun();
        assertNotNull(lastRun, "lastRun must be populated after one run");
        assertEquals(1_234_567_890L, lastRun.timestampMillis(), "timestampMillis");
        assertEquals(7L,             lastRun.removed(),          "removed");
        assertEquals(200L,           lastRun.durationMillis(),   "durationMillis");
        assertEquals(19.8,  lastRun.tpsBefore(), 1e-9, "tpsBefore");
        assertEquals(19.0,  lastRun.tpsAfter(),  1e-9, "tpsAfter");
        assertEquals(-0.8,  lastRun.tpsImpact(), 1e-9, "tpsImpact");
        assertEquals(Map.of("heroes", 3L), lastRun.groupCounts(), "groupCounts");
        assertEquals(Map.of("nether", 7L), lastRun.worldCounts(), "worldCounts");
    }

    @Test
    void singleRun_nullTps_lastRunTpsFieldsAreNull() throws Exception {
        CleanupRunStats run = new CleanupRunStats(
                0L, 0L, 100L,
                null, null, null,
                Map.of(), Map.of());

        tracker.persistRun("cleaner-notps", run);

        CleanupStatsSnapshot snapshot = tracker.getSnapshot("cleaner-notps");
        assertNotNull(snapshot);

        // tpsImpact being null means the sample must not be counted
        assertEquals(0L,  snapshot.totals().tpsSamples(),     "tpsSamples must stay 0");
        assertEquals(0.0, snapshot.totals().tpsImpactTotal(), 1e-9, "tpsImpactTotal must stay 0");

        CleanupStatsSnapshot.LastRun lastRun = snapshot.lastRun();
        assertNotNull(lastRun);
        assertNull(lastRun.tpsBefore(), "tpsBefore must be null");
        assertNull(lastRun.tpsAfter(),  "tpsAfter must be null");
        assertNull(lastRun.tpsImpact(), "tpsImpact must be null");
    }

    // -----------------------------------------------------------------------------------------
    // Multi-run accumulation
    // -----------------------------------------------------------------------------------------

    @Test
    void twoRuns_totals_accumulateCorrectly() throws Exception {
        tracker.persistRun("cleaner-acc", new CleanupRunStats(
                1L, 10L, 300L, 20.0, 19.5, -0.5,
                Map.of("alpha", 4),
                Map.of("nether", 6)));

        tracker.persistRun("cleaner-acc", new CleanupRunStats(
                2L, 30L, 700L, 19.5, 19.0, -0.5,
                Map.of("alpha", 2, "beta", 1),
                Map.of("nether", 4, "end", 3)));

        CleanupStatsSnapshot.Totals totals = tracker.getSnapshot("cleaner-acc").totals();

        assertEquals(2L,     totals.runs(),           "runs");
        assertEquals(40L,    totals.removed(),        "removed (10+30)");
        assertEquals(1_000L, totals.durationMillis(), "durationMillis (300+700)");
        assertEquals(-1.0,   totals.tpsImpactTotal(), 1e-9, "tpsImpactTotal (-0.5 + -0.5)");
        assertEquals(2L,     totals.tpsSamples(),     "tpsSamples");
        // group "alpha" seen in both runs → 4+2=6; "beta" only in run2 → 1
        assertEquals(Map.of("alpha", 6L, "beta", 1L), totals.groupTotals(), "groupTotals");
        // world "nether" → 6+4=10; "end" only in run2 → 3
        assertEquals(Map.of("nether", 10L, "end", 3L), totals.worldTotals(), "worldTotals");
    }

    @Test
    void twoRuns_lastRun_reflectsMostRecentRunOnly() throws Exception {
        tracker.persistRun("cleaner-lr", new CleanupRunStats(
                100L, 5L, 100L, null, null, null,
                Map.of("old-group", 9),
                Map.of("old-world", 9)));

        tracker.persistRun("cleaner-lr", new CleanupRunStats(
                200L, 8L, 250L, 19.9, 19.7, -0.2,
                Map.of("new-group", 3),
                Map.of("new-world", 3)));

        CleanupStatsSnapshot.LastRun lastRun = tracker.getSnapshot("cleaner-lr").lastRun();
        assertNotNull(lastRun);

        assertEquals(200L, lastRun.timestampMillis(), "timestamp must be the second run's");
        assertEquals(8L,   lastRun.removed(),         "removed must be the second run's value");

        assertEquals(Map.of("new-group", 3L), lastRun.groupCounts(),
                "last-run group counts must reflect the second run only");
        assertEquals(Map.of("new-world", 3L), lastRun.worldCounts(),
                "last-run world counts must reflect the second run only");

        assertFalse(lastRun.groupCounts().containsKey("old-group"),
                "first run's group key must not appear in last-run after it is replaced");
        assertFalse(lastRun.worldCounts().containsKey("old-world"),
                "first run's world key must not appear in last-run after it is replaced");
    }

    @Test
    void twoRuns_averageTpsImpact_isComputedCorrectly() throws Exception {
        tracker.persistRun("cleaner-avg", new CleanupRunStats(
                1L, 0L, 100L, 20.0, 19.0, -1.0, Map.of(), Map.of()));
        tracker.persistRun("cleaner-avg", new CleanupRunStats(
                2L, 0L, 100L, 20.0, 18.0, -2.0, Map.of(), Map.of()));

        CleanupStatsSnapshot.Totals totals = tracker.getSnapshot("cleaner-avg").totals();

        assertEquals(-3.0, totals.tpsImpactTotal(), 1e-9, "tpsImpactTotal");
        assertEquals(2L,   totals.tpsSamples(),    "tpsSamples");
        assertNotNull(totals.averageTpsImpact(), "averageTpsImpact must be non-null");
        assertEquals(-1.5, totals.averageTpsImpact(), 1e-9, "averageTpsImpact (-3/2)");
    }

    // -----------------------------------------------------------------------------------------
    // Multi-cleaner isolation
    // -----------------------------------------------------------------------------------------

    @Test
    void multipleCleaners_getAllSnapshots_containsEachCleaner() throws Exception {
        tracker.persistRun("cleaner-x", new CleanupRunStats(1L, 1L, 1L, null, null, null, Map.of(), Map.of()));
        tracker.persistRun("cleaner-y", new CleanupRunStats(2L, 2L, 2L, null, null, null, Map.of(), Map.of()));
        tracker.persistRun("cleaner-z", new CleanupRunStats(3L, 3L, 3L, null, null, null, Map.of(), Map.of()));

        Map<String, CleanupStatsSnapshot> all = tracker.getAllSnapshots();

        assertEquals(3, all.size(), "getAllSnapshots must return one entry per cleaner");
        assertTrue(all.containsKey("cleaner-x"));
        assertTrue(all.containsKey("cleaner-y"));
        assertTrue(all.containsKey("cleaner-z"));
    }

    @Test
    void multipleCleaners_statsAreIsolated_noLeakBetweenCleaners() throws Exception {
        tracker.persistRun("isolated-a", new CleanupRunStats(
                1L, 100L, 500L, 20.0, 19.0, -1.0,
                Map.of("grp", 7), Map.of("wld", 3)));

        tracker.persistRun("isolated-b", new CleanupRunStats(
                2L, 999L, 999L, null, null, null,
                Map.of("other", 99), Map.of("other", 99)));

        CleanupStatsSnapshot snapshotA = tracker.getSnapshot("isolated-a");
        assertNotNull(snapshotA);
        assertEquals(100L, snapshotA.totals().removed(), "cleaner-a removed must not be affected by cleaner-b");
        assertEquals(Map.of("grp", 7L), snapshotA.totals().groupTotals());
        assertEquals(Map.of("wld", 3L), snapshotA.totals().worldTotals());
        assertFalse(snapshotA.totals().groupTotals().containsKey("other"),
                "cleaner-a totals must not contain cleaner-b's keys");
    }
}
