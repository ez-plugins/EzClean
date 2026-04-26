package com.skyblockexp.ezclean.storage.model;

import com.github.ezframework.jaloquent.model.Model;
import org.jetbrains.annotations.Nullable;

/**
 * Jaloquent model for {@code ezclean_last_run}.
 * One row per cleaner; the model ID equals the cleaner ID.
 */
public class LastRunModel extends Model {

    public LastRunModel(String cleanerId) {
        super(cleanerId);
    }

    public long getTimestampMs() {
        return getAs("timestamp_ms", Long.class, 0L);
    }

    public void setTimestampMs(long timestampMs) {
        set("timestamp_ms", timestampMs);
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

    public @Nullable Double getTpsBefore() {
        Object raw = get("tps_before");
        if (raw == null) {
            return null;
        }
        return getAs("tps_before", Double.class);
    }

    public void setTpsBefore(@Nullable Double tpsBefore) {
        set("tps_before", tpsBefore);
    }

    public @Nullable Double getTpsAfter() {
        Object raw = get("tps_after");
        if (raw == null) {
            return null;
        }
        return getAs("tps_after", Double.class);
    }

    public void setTpsAfter(@Nullable Double tpsAfter) {
        set("tps_after", tpsAfter);
    }

    public @Nullable Double getTpsImpact() {
        Object raw = get("tps_impact");
        if (raw == null) {
            return null;
        }
        return getAs("tps_impact", Double.class);
    }

    public void setTpsImpact(@Nullable Double tpsImpact) {
        set("tps_impact", tpsImpact);
    }
}
