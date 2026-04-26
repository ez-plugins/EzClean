package com.skyblockexp.ezclean.storage.model;

import com.github.ezframework.jaloquent.model.Model;

/**
 * Jaloquent model for {@code ezclean_stats_totals}.
 * One row per cleaner; the model ID equals the cleaner ID.
 */
public class CleanerTotalsModel extends Model {

    public CleanerTotalsModel(String cleanerId) {
        super(cleanerId);
    }

    public long getRuns() {
        return getAs("runs", Long.class, 0L);
    }

    public void setRuns(long runs) {
        set("runs", runs);
    }

    public long getRemoved() {
        return getAs("removed", Long.class, 0L);
    }

    public void setRemoved(long removed) {
        set("removed", removed);
    }

    public long getDurationMs() {
        return getAs("duration_ms", Long.class, 0L);
    }

    public void setDurationMs(long durationMs) {
        set("duration_ms", durationMs);
    }

    public double getTpsImpactTotal() {
        return getAs("tps_impact_total", Double.class, 0.0);
    }

    public void setTpsImpactTotal(double tpsImpactTotal) {
        set("tps_impact_total", tpsImpactTotal);
    }

    public long getTpsSamples() {
        return getAs("tps_samples", Long.class, 0L);
    }

    public void setTpsSamples(long tpsSamples) {
        set("tps_samples", tpsSamples);
    }
}
