package com.skyblockexp.ezclean.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.skyblockexp.ezclean.EzCleanPlugin;

/**
 * Handles loading EzClean configuration data from the split configuration files.
 */
public final class EzCleanConfigurationLoader {

    private static final String DEATH_CHESTS_FILE_NAME = "death-chests.yml";
    private static final String MESSAGES_FILE_NAME = "messages.yml";
    private static final String CLEANERS_DIRECTORY_NAME = "cleaners";
    private static final String DEFAULT_CLEANER_RESOURCE = "cleaners/default.yml";

    private final EzCleanPlugin plugin;
    private final Logger logger;

    public EzCleanConfigurationLoader(EzCleanPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public FileConfiguration loadConfiguration() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Unable to create the EzClean data folder; continuing with in-memory defaults.");
        }

        migrateLegacyConfigurationIfNecessary();
        ensureDefaultSplitConfiguration();

        File deathChestFile = new File(dataFolder, DEATH_CHESTS_FILE_NAME);
        File messagesFile = new File(dataFolder, MESSAGES_FILE_NAME);
        File cleanersDirectory = new File(dataFolder, CLEANERS_DIRECTORY_NAME);

        return assembleConfiguration(deathChestFile, cleanersDirectory, messagesFile);
    }

    private void migrateLegacyConfigurationIfNecessary() {
        File dataFolder = plugin.getDataFolder();
        File legacyConfigFile = new File(dataFolder, "config.yml");
        if (!legacyConfigFile.exists()) {
            return;
        }

        YamlConfiguration legacyConfiguration = YamlConfiguration.loadConfiguration(legacyConfigFile);

        File deathChestFile = new File(dataFolder, DEATH_CHESTS_FILE_NAME);
        if (!deathChestFile.exists()) {
            ConfigurationSection deathChestSection = legacyConfiguration.getConfigurationSection("death-chests");
            if (deathChestSection != null && !deathChestSection.getKeys(false).isEmpty()) {
                YamlConfiguration output = new YamlConfiguration();
                copySection(deathChestSection, output);
                saveYaml(output, deathChestFile, "death chest settings");
            }
        }

        File cleanersDirectory = new File(dataFolder, CLEANERS_DIRECTORY_NAME);
        if (!cleanersDirectory.exists() && !cleanersDirectory.mkdirs()) {
            logger.warning("Unable to create the EzClean cleaners directory while migrating legacy configuration.");
            return;
        }

        if (hasYamlFiles(cleanersDirectory)) {
            return;
        }

        ConfigurationSection cleanersSection = legacyConfiguration.getConfigurationSection("cleaners");
        if (cleanersSection != null && !cleanersSection.getKeys(false).isEmpty()) {
            for (String cleanerId : cleanersSection.getKeys(false)) {
                ConfigurationSection cleanerSection = cleanersSection.getConfigurationSection(cleanerId);
                if (cleanerSection == null || cleanerSection.getKeys(true).isEmpty()) {
                    continue;
                }

                File cleanerFile = new File(cleanersDirectory, toCleanerFileName(cleanerId));
                if (cleanerFile.exists()) {
                    continue;
                }

                YamlConfiguration output = new YamlConfiguration();
                copySection(cleanerSection, output);
                saveYaml(output, cleanerFile, String.format(Locale.ROOT, "cleanup profile '%s'", cleanerId));
            }
            return;
        }

        ConfigurationSection legacyCleanupSection = legacyConfiguration.getConfigurationSection("cleanup");
        if (legacyCleanupSection != null && !legacyCleanupSection.getKeys(false).isEmpty()) {
            File cleanerFile = new File(cleanersDirectory, "default.yml");
            if (!cleanerFile.exists()) {
                YamlConfiguration output = new YamlConfiguration();
                copySection(legacyCleanupSection, output);
                saveYaml(output, cleanerFile, "legacy cleanup profile");
            }
        }
    }

    private void ensureDefaultSplitConfiguration() {
        File dataFolder = plugin.getDataFolder();

        File configNotice = new File(dataFolder, "config.yml");
        if (!configNotice.exists()) {
            plugin.saveResource("config.yml", false);
        }

        File deathChestFile = new File(dataFolder, DEATH_CHESTS_FILE_NAME);
        if (!deathChestFile.exists()) {
            plugin.saveResource(DEATH_CHESTS_FILE_NAME, false);
        }

        File cleanersDirectory = new File(dataFolder, CLEANERS_DIRECTORY_NAME);
        if (!cleanersDirectory.exists() && !cleanersDirectory.mkdirs()) {
            logger.warning("Unable to create the EzClean cleaners directory.");
            return;
        }

        if (!hasYamlFiles(cleanersDirectory)) {
            plugin.saveResource(DEFAULT_CLEANER_RESOURCE, false);
        }

        File messagesFile = new File(dataFolder, MESSAGES_FILE_NAME);
        if (!messagesFile.exists()) {
            plugin.saveResource(MESSAGES_FILE_NAME, false);
        }
    }

    private FileConfiguration assembleConfiguration(File deathChestFile, File cleanersDirectory, File messagesFile) {
        YamlConfiguration combined = new YamlConfiguration();

        YamlConfiguration deathChestConfiguration = YamlConfiguration.loadConfiguration(deathChestFile);
        copySection(deathChestConfiguration, combined.createSection("death-chests"));

        if (messagesFile.isFile()) {
            YamlConfiguration messagesConfiguration = YamlConfiguration.loadConfiguration(messagesFile);
            copySection(messagesConfiguration, combined.createSection("messages"));
        } else {
            logger.warning("EzClean messages.yml was not found; using built-in message defaults.");
        }

        ConfigurationSection cleanersSection = combined.createSection("cleaners");
        File[] cleanerFiles = cleanersDirectory.listFiles(file -> file.isFile() && isYamlFile(file.getName()));
        if (cleanerFiles == null || cleanerFiles.length == 0) {
            logger.warning("No EzClean cleaner profiles were found; using built-in defaults.");
            return combined;
        }

        Arrays.sort(cleanerFiles, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File cleanerFile : cleanerFiles) {
            YamlConfiguration cleanerConfiguration = YamlConfiguration.loadConfiguration(cleanerFile);
            String cleanerId = fileNameWithoutExtension(cleanerFile.getName());
            ConfigurationSection cleanerSection = cleanersSection.createSection(cleanerId);
            copySection(cleanerConfiguration, cleanerSection);
        }

        return combined;
    }

    private static void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            if (source.isConfigurationSection(key)) {
                ConfigurationSection childTarget = target.createSection(key);
                copySection(source.getConfigurationSection(key), childTarget);
            } else {
                target.set(key, source.get(key));
            }
        }
    }

    private void saveYaml(YamlConfiguration configuration, File destination, String description) {
        try {
            configuration.save(destination);
            logger.info(() -> String.format(Locale.ROOT, "Migrated %s to %s.", description, destination.getName()));
        } catch (IOException ex) {
            logger.log(Level.WARNING,
                    String.format(Locale.ROOT, "Failed to save %s while migrating EzClean configuration.", description),
                    ex);
        }
    }

    private static boolean hasYamlFiles(File directory) {
        File[] files = directory.listFiles(file -> file.isFile() && isYamlFile(file.getName()));
        return files != null && files.length > 0;
    }

    private static boolean isYamlFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yml") || lower.endsWith(".yaml");
    }

    private static String toCleanerFileName(String cleanerId) {
        String sanitized = cleanerId.replaceAll("[^A-Za-z0-9-_]+", "-");
        if (sanitized.isBlank()) {
            sanitized = "cleaner";
        }
        return sanitized + ".yml";
    }

    private static String fileNameWithoutExtension(String name) {
        int index = name.lastIndexOf('.');
        return index > 0 ? name.substring(0, index) : name;
    }
}
