package com.skyblockexp.ezclean.storage;

import com.github.ezframework.jaloquent.config.DatabaseSettings;
import com.github.ezframework.jaloquent.config.JaloquentConfig;
import com.github.ezframework.jaloquent.config.JdbcScheme;
import com.github.ezframework.jaloquent.exception.MigrationException;
import com.github.ezframework.jaloquent.migration.MigrationRunner;
import com.github.ezframework.jaloquent.model.BaseModel;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.sql.SqlDialect;
import com.skyblockexp.ezclean.storage.migration.CreateStatsTables;
import com.skyblockexp.ezclean.storage.model.CleanerTotalsModel;
import com.skyblockexp.ezclean.storage.model.CountModel;
import com.skyblockexp.ezclean.storage.model.LastRunModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Initialises the Jaloquent persistence layer for EzClean statistics storage.
 *
 * <p>Reads the {@code storage} block from {@code config.yml}, sets up the JDBC data source,
 * runs any pending schema migrations, registers tables with {@link TableRegistry}, and
 * exposes typed {@link ModelRepository} instances for the three stats tables.
 */
public final class StorageService {

    private final DataSourceJdbcStore store;
    private final ModelRepository<CleanerTotalsModel> totalsRepo;
    private final ModelRepository<LastRunModel> lastRunRepo;
    private final ModelRepository<CountModel> countsRepo;

    private StorageService(
            DataSourceJdbcStore store,
            ModelRepository<CleanerTotalsModel> totalsRepo,
            ModelRepository<LastRunModel> lastRunRepo,
            ModelRepository<CountModel> countsRepo) {
        this.store      = store;
        this.totalsRepo = totalsRepo;
        this.lastRunRepo = lastRunRepo;
        this.countsRepo = countsRepo;
    }

    /**
     * Builds and initialises a {@code StorageService} from the plugin configuration.
     *
     * @param config     the plugin's assembled {@link FileConfiguration}
     * @param dataFolder the plugin data folder (used to resolve the SQLite file path)
     * @param logger     logger for startup messages and errors
     * @return a fully initialised storage service
     * @throws StorageException if the database cannot be opened or migrations fail
     */
    public static StorageService create(FileConfiguration config, File dataFolder, Logger logger)
            throws StorageException {

        JaloquentConfig.enableLogging(false);
        JaloquentConfig.enableMetrics(false);

        DataSourceJdbcStore store = buildStore(config, dataFolder, logger);

        runMigrations(store, logger);

        registerTables();

        ModelRepository<CleanerTotalsModel> totalsRepo = new ModelRepository<>(
                store, "ezclean_stats_totals",
                (id, data) -> {
                    CleanerTotalsModel m = new CleanerTotalsModel(id);
                    m.fromMap(data);
                    return m;
                });

        ModelRepository<LastRunModel> lastRunRepo = new ModelRepository<>(
                store, "ezclean_last_run",
                (id, data) -> {
                    LastRunModel m = new LastRunModel(id);
                    m.fromMap(data);
                    return m;
                });

        ModelRepository<CountModel> countsRepo = new ModelRepository<>(
                store, "ezclean_counts",
                (id, data) -> {
                    CountModel m = new CountModel(id);
                    m.fromMap(data);
                    return m;
                });

        return new StorageService(store, totalsRepo, lastRunRepo, countsRepo);
    }

    // -----------------------------------------------------------------------------------------
    // Repositories
    // -----------------------------------------------------------------------------------------

    /**
     * Persists a model to the given table using a dialect-neutral {@code REPLACE INTO}
     * upsert that works for both SQLite and MySQL.
     *
     * <p>This is the preferred save path because Jaloquent's {@code ModelRepository.save()}
     * generates MySQL-specific {@code ON DUPLICATE KEY UPDATE} SQL, which SQLite rejects.
     *
     * @param tableName registered table name ({@link TableRegistry} key)
     * @param model     model to persist
     * @throws com.github.ezframework.jaloquent.exception.StorageException on JDBC error
     */
    public void saveModel(String tableName, BaseModel model)
            throws com.github.ezframework.jaloquent.exception.StorageException {

        TableRegistry.TableMeta meta = TableRegistry.get(tableName);
        if (meta == null) {
            throw new com.github.ezframework.jaloquent.exception.StorageException(
                    "Table '" + tableName + "' is not registered in TableRegistry.", null);
        }

        Map<String, Object> data = model.toMap();

        List<String> cols   = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        // id always comes first, matching Model.save() convention
        cols.add("id");
        params.add(model.getId());

        for (String col : meta.columns().keySet()) {
            if ("id".equals(col)) continue;
            cols.add(col);
            params.add(data.get(col));
        }

        String colList      = String.join(", ", cols);
        String placeholders = String.join(", ", Collections.nCopies(cols.size(), "?"));
        // REPLACE INTO is supported by both SQLite (as INSERT OR REPLACE alias) and MySQL
        String sql          = "REPLACE INTO " + tableName + " (" + colList + ") VALUES (" + placeholders + ")";

        try {
            store.executeUpdate(sql, params);
        } catch (Exception ex) {
            throw new com.github.ezframework.jaloquent.exception.StorageException(
                    "Failed to save model '" + model.getId() + "' to table '" + tableName + "'.", ex);
        }
    }

    public ModelRepository<CleanerTotalsModel> getTotalsRepo() {
        return totalsRepo;
    }

    public ModelRepository<LastRunModel> getLastRunRepo() {
        return lastRunRepo;
    }

    public ModelRepository<CountModel> getCountsRepo() {
        return countsRepo;
    }

    // -----------------------------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------------------------

    private static DataSourceJdbcStore buildStore(FileConfiguration config, File dataFolder, Logger logger)
            throws StorageException {

        ConfigurationSection storageSection = config.getConfigurationSection("storage");
        String type = storageSection != null
                ? storageSection.getString("type", "sqlite").toLowerCase(java.util.Locale.ROOT)
                : "sqlite";

        DatabaseSettings settings;
        if ("mysql".equals(type)) {
            ConfigurationSection mysql = storageSection != null
                    ? storageSection.getConfigurationSection("mysql")
                    : null;
            String host     = mysql != null ? mysql.getString("host", "localhost") : "localhost";
            int    port     = mysql != null ? mysql.getInt("port", 3306) : 3306;
            String database = mysql != null ? mysql.getString("database", "ezclean") : "ezclean";
            String username = mysql != null ? mysql.getString("username", "root") : "root";
            String password = mysql != null ? mysql.getString("password", "") : "";

            settings = DatabaseSettings.builder()
                    .jdbcScheme(JdbcScheme.MYSQL)
                    .host(host)
                    .port(port)
                    .databaseName(database)
                    .username(username)
                    .password(password)
                    .build();

            logger.info("[EzClean Storage] Using MySQL at " + host + ":" + port + "/" + database);
        } else {
            ConfigurationSection sqlite = storageSection != null
                    ? storageSection.getConfigurationSection("sqlite")
                    : null;
            String fileName = sqlite != null
                    ? sqlite.getString("file", "ezclean-stats.db")
                    : "ezclean-stats.db";

            File dbFile = new File(dataFolder, fileName);
            settings = DatabaseSettings.builder()
                    .jdbcScheme(JdbcScheme.SQLITE)
                    .url("jdbc:sqlite:" + dbFile.getAbsolutePath())
                    .build();

            logger.info("[EzClean Storage] Using SQLite at " + dbFile.getAbsolutePath());
        }

        try {
            JaloquentConfig.setDatabaseSettings(settings);
            return (DataSourceJdbcStore) JaloquentConfig.buildStore();
        } catch (Exception ex) {
            throw new StorageException("Failed to open database connection.", ex);
        }
    }

    private static void runMigrations(DataSourceJdbcStore store, Logger logger) throws StorageException {
        MigrationRunner runner = new MigrationRunner(
                store,
                SqlDialect.STANDARD,
                List.of(new CreateStatsTables()));
        try {
            runner.run();
        } catch (MigrationException ex) {
            throw new StorageException("Database migration failed.", ex);
        }
        logger.fine("[EzClean Storage] Migrations applied successfully.");
    }

    private static void registerTables() {
        TableRegistry.register("ezclean_stats_totals", "ezclean_stats_totals", Map.of(
                "id",               "VARCHAR(64) PRIMARY KEY",
                "runs",             "BIGINT NOT NULL DEFAULT 0",
                "removed",          "BIGINT NOT NULL DEFAULT 0",
                "duration_ms",      "BIGINT NOT NULL DEFAULT 0",
                "tps_impact_total", "DOUBLE NOT NULL DEFAULT 0",
                "tps_samples",      "BIGINT NOT NULL DEFAULT 0"
        ));

        TableRegistry.register("ezclean_last_run", "ezclean_last_run", Map.of(
                "id",           "VARCHAR(64) PRIMARY KEY",
                "timestamp_ms", "BIGINT NOT NULL DEFAULT 0",
                "removed",      "BIGINT NOT NULL DEFAULT 0",
                "duration_ms",  "BIGINT NOT NULL DEFAULT 0",
                "tps_before",   "DOUBLE",
                "tps_after",    "DOUBLE",
                "tps_impact",   "DOUBLE"
        ));

        TableRegistry.register("ezclean_counts", "ezclean_counts", Map.of(
                "id",         "VARCHAR(200) PRIMARY KEY",
                "cleaner_id", "VARCHAR(64) NOT NULL",
                "count_type", "VARCHAR(20) NOT NULL",
                "key_name",   "VARCHAR(100) NOT NULL",
                "count",      "BIGINT NOT NULL DEFAULT 0"
        ));
    }
}
