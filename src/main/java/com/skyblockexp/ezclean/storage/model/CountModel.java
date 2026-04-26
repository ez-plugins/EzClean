package com.skyblockexp.ezclean.storage.model;

import com.github.ezframework.jaloquent.model.Model;

/**
 * Jaloquent model for {@code ezclean_counts}.
 *
 * <p>Covers all four count maps (cleaner group totals, cleaner world totals,
 * last-run group counts, last-run world counts).
 *
 * <p>The model ID is a composite key: {@code {cleanerId}|{countType}|{keyName}}.
 * Use {@link #buildId(String, CountType, String)} to construct it.
 */
public class CountModel extends Model {

    /**
     * Named discriminator for the four map categories stored in this table.
     */
    public enum CountType {
        TOTAL_GROUP,
        TOTAL_WORLD,
        LAST_RUN_GROUP,
        LAST_RUN_WORLD
    }

    public CountModel(String id) {
        super(id);
    }

    /**
     * Builds the composite primary key used as the row ID.
     */
    public static String buildId(String cleanerId, CountType type, String keyName) {
        return cleanerId + "|" + type.name() + "|" + keyName;
    }

    public static CountModel create(String cleanerId, CountType type, String keyName, long count) {
        CountModel model = new CountModel(buildId(cleanerId, type, keyName));
        model.setCleanerId(cleanerId);
        model.setCountType(type.name());
        model.setKeyName(keyName);
        model.setCount(count);
        return model;
    }

    public String getCleanerId() {
        return getAs("cleaner_id", String.class, "");
    }

    public void setCleanerId(String cleanerId) {
        set("cleaner_id", cleanerId);
    }

    public String getCountType() {
        return getAs("count_type", String.class, "");
    }

    public void setCountType(String countType) {
        set("count_type", countType);
    }

    public String getKeyName() {
        return getAs("key_name", String.class, "");
    }

    public void setKeyName(String keyName) {
        set("key_name", keyName);
    }

    public long getCount() {
        return getAs("count", Long.class, 0L);
    }

    public void setCount(long count) {
        set("count", count);
    }
}
