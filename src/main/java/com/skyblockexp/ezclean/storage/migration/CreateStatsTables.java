package com.skyblockexp.ezclean.storage.migration;

import com.github.ezframework.jaloquent.migration.Migration;
import com.github.ezframework.jaloquent.migration.Schema;
import com.github.ezframework.jaloquent.exception.MigrationException;
import com.github.ezframework.javaquerybuilder.query.builder.col.BigInt;
import com.github.ezframework.javaquerybuilder.query.builder.col.VarChar;
import com.github.ezframework.javaquerybuilder.query.builder.ColumnType;

/**
 * Creates the three EzClean statistics tables:
 * <ul>
 *   <li>{@code ezclean_stats_totals} – cumulative per-cleaner counters</li>
 *   <li>{@code ezclean_last_run}     – most-recent-run record per cleaner</li>
 *   <li>{@code ezclean_counts}       – group / world count rows for both totals and last-run</li>
 * </ul>
 */
public class CreateStatsTables implements Migration {

    @Override
    public String getId() {
        return "2026_04_23_001_create_stats_tables";
    }

    @Override
    public void up(Schema schema) throws MigrationException {
        schema.create("ezclean_stats_totals", t -> t
                .ifNotExists()
                .string("id", 64)
                .primaryKey("id")
                .column("runs",             BigInt.of().notNull().defaultValue("0"))
                .column("removed",          BigInt.of().notNull().defaultValue("0"))
                .column("duration_ms",      BigInt.of().notNull().defaultValue("0"))
                .column("tps_impact_total", ColumnType.DOUBLE.notNull().defaultValue("0"))
                .column("tps_samples",      BigInt.of().notNull().defaultValue("0"))
        );

        schema.create("ezclean_last_run", t -> t
                .ifNotExists()
                .string("id", 64)
                .primaryKey("id")
                .column("timestamp_ms", BigInt.of().notNull().defaultValue("0"))
                .column("removed",      BigInt.of().notNull().defaultValue("0"))
                .column("duration_ms",  BigInt.of().notNull().defaultValue("0"))
                .column("tps_before",   ColumnType.DOUBLE)
                .column("tps_after",    ColumnType.DOUBLE)
                .column("tps_impact",   ColumnType.DOUBLE)
        );

        schema.create("ezclean_counts", t -> t
                .ifNotExists()
                .string("id", 200)
                .primaryKey("id")
                .column("cleaner_id",  VarChar.of(64).notNull())
                .column("count_type",  VarChar.of(20).notNull())
                .column("key_name",    VarChar.of(100).notNull())
                .column("count",       BigInt.of().notNull().defaultValue("0"))
        );
    }

    @Override
    public void down(Schema schema) throws MigrationException {
        schema.dropIfExists("ezclean_counts");
        schema.dropIfExists("ezclean_last_run");
        schema.dropIfExists("ezclean_stats_totals");
    }
}
