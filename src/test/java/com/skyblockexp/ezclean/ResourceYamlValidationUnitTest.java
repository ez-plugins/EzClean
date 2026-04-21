package com.skyblockexp.ezclean;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates that all resource YAML files bundled with the plugin are well-formed and contain
 * the expected top-level keys. Failures here indicate a misconfiguration that would cause
 * silent runtime errors or misleading defaults.
 */
class ResourceYamlValidationUnitTest {

    @Test
    void configYamlLoadsWithoutError() {
        YamlConfiguration config = loadResource("config.yml");
        assertTrue(config.contains("update-check"), "config.yml must declare update-check section");
        assertTrue(config.contains("update-check.enabled"), "config.yml must declare update-check.enabled");
    }

    @Test
    void cleanerDefaultYamlIsValid() {
        YamlConfiguration config = loadResource("cleaners/default.yml");
        assertTrue(config.contains("interval-minutes"), "cleaners/default.yml must declare interval-minutes");
        assertTrue(config.contains("warning"), "cleaners/default.yml must declare warning section");
        assertTrue(config.contains("broadcast"), "cleaners/default.yml must declare broadcast section");
        assertTrue(config.contains("cancel"), "cleaners/default.yml must declare cancel section");
        assertTrue(config.contains("worlds"), "cleaners/default.yml must declare worlds list");
        assertTrue(config.contains("pile-detection"), "cleaners/default.yml must declare pile-detection section");
        assertTrue(config.contains("remove"), "cleaners/default.yml must declare remove section");
        assertTrue(config.contains("protect"), "cleaners/default.yml must declare protect section");
        assertTrue(config.contains("performance"), "cleaners/default.yml must declare performance section");
        assertTrue(config.contains("performance.async-removal"),
                "performance.async-removal must be present in cleaners/default.yml");
        assertFalse(config.getBoolean("performance.async-removal"),
                "performance.async-removal should default to false");
    }

    @Test
    void deathChestsYamlLoadsWithoutError() {
        YamlConfiguration config = loadResource("death-chests.yml");
        assertNotNull(config, "death-chests.yml must be present on the classpath");
    }

    @Test
    void messagesYamlLoadsWithoutError() {
        YamlConfiguration config = loadResource("messages.yml");
        assertNotNull(config, "messages.yml must be present on the classpath");
    }

    @Test
    void pluginYamlHasRequiredFields() {
        YamlConfiguration config = loadResource("plugin.yml");
        assertTrue(config.contains("name"), "plugin.yml must declare name");
        assertTrue(config.contains("main"), "plugin.yml must declare main class");
        assertTrue(config.contains("commands"), "plugin.yml must declare commands section");
        assertTrue(config.contains("permissions"), "plugin.yml must declare permissions section");
        assertTrue(config.contains("permissions.ezclean.toggle"),
                "plugin.yml must declare ezclean.toggle permission");
    }

    // -------------------------------------------------------------------------

    private YamlConfiguration loadResource(String resourcePath) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(stream, "Resource not found on classpath: " + resourcePath);
        return assertDoesNotThrow(
                () -> {
                    try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        return YamlConfiguration.loadConfiguration(reader);
                    }
                },
                "Failed to parse YAML resource: " + resourcePath
        );
    }
}
