package com.skyblockexp.ezclean.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable filter that protects dropped {@link org.bukkit.entity.Item} entities whose
 * held {@link ItemStack} matches at least one of the configured metadata rules:
 * <ul>
 *   <li><b>custom-model-data</b> — protect items with a specific custom model data value
 *       or any CMD value when the wildcard {@code *} is used.</li>
 *   <li><b>lore-contains</b> — protect items whose lore contains a given string
 *       (case-insensitive, legacy colour codes stripped).</li>
 *   <li><b>pdc-keys</b> — protect items whose persistent data container holds a given
 *       key (presence check, type-independent).</li>
 * </ul>
 *
 * <p>Evaluation occurs before the standard {@code dropped-items} removal decision in
 * {@link com.skyblockexp.ezclean.scheduler.RemovalEvaluator}; a match causes the item
 * to be skipped regardless of the {@code remove.dropped-items} setting.
 */
public final class ItemMetadataFilter {

    private static final ItemMetadataFilter DISABLED = new ItemMetadataFilter(
            false, false, Collections.emptySet(), Collections.emptyList(), Collections.emptySet());

    // Matches Bukkit legacy color-code prefix § and the following character
    private static final java.util.regex.Pattern LEGACY_COLOR_PATTERN =
            java.util.regex.Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    private final boolean enabled;
    /** {@code true} when the wildcard {@code *} was listed for custom-model-data. */
    private final boolean anyCustomModelData;
    private final Set<Integer> customModelDataValues;
    /** Each entry is a lowercase substring that must appear in at least one lore line. */
    private final List<String> loreContains;
    private final Set<NamespacedKey> pdcKeys;

    private ItemMetadataFilter(boolean enabled, boolean anyCustomModelData,
            Set<Integer> customModelDataValues, List<String> loreContains,
            Set<NamespacedKey> pdcKeys) {
        this.enabled = enabled;
        this.anyCustomModelData = anyCustomModelData;
        this.customModelDataValues = customModelDataValues;
        this.loreContains = loreContains;
        this.pdcKeys = pdcKeys;
    }

    /** Returns a disabled filter that never protects any item. */
    public static ItemMetadataFilter disabled() {
        return DISABLED;
    }

    /**
     * Parses item-metadata protection rules from the given configuration section.
     *
     * @param section the {@code protect.item-metadata} config section, or {@code null}
     * @param logger  plugin logger used to report parse errors
     * @param path    config path prefix used in warning messages
     * @return parsed filter, or {@link #disabled()} when the section is absent or
     *         {@code enabled: false}
     */
    public static ItemMetadataFilter parse(@Nullable ConfigurationSection section,
            @Nullable Logger logger, String path) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return DISABLED;
        }

        boolean anyCmd = false;
        Set<Integer> cmdValues = new HashSet<>();
        for (String entry : section.getStringList("custom-model-data")) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            if ("*".equals(trimmed)) {
                anyCmd = true;
                cmdValues.clear();
                break;
            }
            try {
                cmdValues.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ex) {
                if (logger != null) {
                    logger.warning("Invalid custom-model-data value at '" + path + "': " + entry);
                }
            }
        }

        List<String> loreContains = new ArrayList<>();
        for (String line : section.getStringList("lore-contains")) {
            if (line != null && !line.isBlank()) {
                loreContains.add(line.toLowerCase(Locale.ROOT));
            }
        }

        Set<NamespacedKey> pdcKeys = new HashSet<>();
        for (String raw : section.getStringList("pdc-keys")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            int colon = raw.indexOf(':');
            if (colon <= 0 || colon >= raw.length() - 1) {
                if (logger != null) {
                    logger.warning("Invalid PDC key at '" + path
                            + "' (expected namespace:key): " + raw);
                }
                continue;
            }
            String namespace = raw.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String key = raw.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
            try {
                pdcKeys.add(new NamespacedKey(namespace, key));
            } catch (IllegalArgumentException ex) {
                if (logger != null) {
                    logger.warning("Invalid PDC key at '" + path + "': " + raw
                            + " — " + ex.getMessage());
                }
            }
        }

        if (!anyCmd && cmdValues.isEmpty() && loreContains.isEmpty() && pdcKeys.isEmpty()) {
            return DISABLED;
        }

        return new ItemMetadataFilter(true, anyCmd,
                Collections.unmodifiableSet(cmdValues),
                Collections.unmodifiableList(loreContains),
                Collections.unmodifiableSet(pdcKeys));
    }

    /** Returns {@code true} if this filter has at least one active rule. */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns {@code true} if the given item stack matches at least one protection rule.
     *
     * @param stack the item stack to evaluate; may be {@code null}
     * @return {@code true} when the item should be protected from removal
     */
    public boolean isProtected(@Nullable ItemStack stack) {
        if (!enabled || stack == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (anyCustomModelData && meta.hasCustomModelData()) {
            return true;
        }
        if (!customModelDataValues.isEmpty() && meta.hasCustomModelData()
                && customModelDataValues.contains(meta.getCustomModelData())) {
            return true;
        }

        if (!loreContains.isEmpty()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String loreLine : lore) {
                    if (loreLine == null) {
                        continue;
                    }
                    String stripped = stripLegacyColors(loreLine).toLowerCase(Locale.ROOT);
                    for (String pattern : loreContains) {
                        if (stripped.contains(pattern)) {
                            return true;
                        }
                    }
                }
            }
        }

        if (!pdcKeys.isEmpty()) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (NamespacedKey key : pdcKeys) {
                if (pdc.has(key)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String stripLegacyColors(String text) {
        return LEGACY_COLOR_PATTERN.matcher(text).replaceAll("");
    }
}
