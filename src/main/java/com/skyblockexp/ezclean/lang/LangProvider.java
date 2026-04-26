package com.skyblockexp.ezclean.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides translated strings for a configured language code.
 *
 * <p>Translations are loaded from bundled jar resources ({@code lang/{code}.yml}).
 * An optional on-disk override file ({@code plugins/EzClean/lang/{code}.yml}) is merged
 * on top, allowing server owners to customise strings without modifying the jar.
 * Missing keys fall back to English ({@code en.yml}).</p>
 */
public final class LangProvider {

    /** No-op instance returned before the plugin is fully loaded. Always returns {@code ""}. */
    public static final LangProvider EMPTY = new LangProvider(Map.of(), Map.of());

    private final Map<String, String> translations;
    private final Map<String, String> fallback;

    private LangProvider(Map<String, String> translations, Map<String, String> fallback) {
        this.translations = translations;
        this.fallback = fallback;
    }

    /**
     * Loads the language provider for the given language code.
     *
     * @param plugin the plugin instance (used to access bundled resources and data folder)
     * @param code   the ISO 639-1 language code, e.g. {@code "en"}, {@code "nl"}, {@code "es"}
     * @return a {@link LangProvider} backed by the requested language with English fallback
     */
    public static LangProvider load(JavaPlugin plugin, String code) {
        Map<String, String> active = loadResource(plugin, code);
        Map<String, String> english = "en".equals(code) ? active : loadResource(plugin, "en");

        // Merge user override file if present (plugins/EzClean/lang/{code}.yml)
        File override = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (override.exists()) {
            YamlConfiguration overrideCfg = YamlConfiguration.loadConfiguration(override);
            for (String key : overrideCfg.getKeys(true)) {
                if (overrideCfg.isString(key)) {
                    active.put(key, overrideCfg.getString(key));
                }
            }
        }

        return new LangProvider(active, english);
    }

    /**
     * Returns the translated string for the given dot-separated key.
     * Falls back to English, then returns {@code ""} if not found.
     */
    public String get(String key) {
        String value = translations.get(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = fallback.get(key);
        return value != null ? value : "";
    }

    /**
     * Returns the translated string with {@code {placeholder}} substitutions applied.
     *
     * @param key      the language key
     * @param kvPairs  alternating placeholder names and values, e.g. {@code "id", cleanerId}
     * @return translated and substituted string
     */
    public String get(String key, String... kvPairs) {
        String value = get(key);
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            value = value.replace("{" + kvPairs[i] + "}", kvPairs[i + 1]);
        }
        return value;
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private static Map<String, String> loadResource(JavaPlugin plugin, String code) {
        InputStream is = plugin.getResource("lang/" + code + ".yml");
        if (is == null) {
            plugin.getLogger().warning("[EzClean] Missing bundled language file: lang/" + code + ".yml");
            return new HashMap<>();
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(
                new InputStreamReader(is, StandardCharsets.UTF_8));
        Map<String, String> map = new HashMap<>();
        for (String key : cfg.getKeys(true)) {
            if (cfg.isString(key)) {
                map.put(key, cfg.getString(key));
            }
        }
        return map;
    }
}
