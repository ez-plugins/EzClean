package com.skyblockexp.ezclean.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemMetadataFilterTest {

    // -----------------------------------------------------------------------
    // disabled() / section absent
    // -----------------------------------------------------------------------

    @Test
    void disabledFilterProtectsNothing() {
        ItemMetadataFilter filter = ItemMetadataFilter.disabled();
        assertFalse(filter.isEnabled());
        assertFalse(filter.isProtected(null));
    }

    @Test
    void nullSectionReturnsDisabled() {
        ItemMetadataFilter filter = ItemMetadataFilter.parse(null, null, "test");
        assertFalse(filter.isEnabled());
    }

    @Test
    void enabledFalseReturnsDisabled() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", false);
        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, null, "test");
        assertFalse(filter.isEnabled());
    }

    // -----------------------------------------------------------------------
    // Custom model data — wildcard
    // -----------------------------------------------------------------------

    @Test
    void wildcardCmdProtectsAnyItemWithCmd() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("custom-model-data", List.of("*"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, Logger.getLogger("test"), "test");
        assertTrue(filter.isEnabled());

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(true);
        Mockito.when(meta.getCustomModelData()).thenReturn(42);
        Mockito.when(meta.getLore()).thenReturn(null);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(Mockito.mock(PersistentDataContainer.class));

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertTrue(filter.isProtected(stack));
    }

    @Test
    void wildcardCmdDoesNotProtectItemWithoutCmd() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("custom-model-data", List.of("*"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, null, "test");

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(false);
        Mockito.when(meta.getLore()).thenReturn(null);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(Mockito.mock(PersistentDataContainer.class));

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertFalse(filter.isProtected(stack));
    }

    // -----------------------------------------------------------------------
    // Custom model data — specific value
    // -----------------------------------------------------------------------

    @Test
    void specificCmdProtectsMatchingValue() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("custom-model-data", List.of("1001", "2002"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, null, "test");

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(true);
        Mockito.when(meta.getCustomModelData()).thenReturn(1001);
        Mockito.when(meta.getLore()).thenReturn(null);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(Mockito.mock(PersistentDataContainer.class));

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertTrue(filter.isProtected(stack));
    }

    @Test
    void specificCmdDoesNotProtectDifferentValue() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("custom-model-data", List.of("1001"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, null, "test");

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(true);
        Mockito.when(meta.getCustomModelData()).thenReturn(9999);
        Mockito.when(meta.getLore()).thenReturn(null);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(Mockito.mock(PersistentDataContainer.class));

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertFalse(filter.isProtected(stack));
    }

    // -----------------------------------------------------------------------
    // Lore-contains
    // -----------------------------------------------------------------------

    @Test
    void loreContainsMatchesCaseInsensitive() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("lore-contains", List.of("soulbound"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, null, "test");

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(false);
        Mockito.when(meta.getLore()).thenReturn(List.of("§aSoulbound to §bPlayer"));
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(Mockito.mock(PersistentDataContainer.class));

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertTrue(filter.isProtected(stack));
    }

    @Test
    void loreContainsDoesNotMatchUnrelatedLore() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("lore-contains", List.of("soulbound"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, null, "test");

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(false);
        Mockito.when(meta.getLore()).thenReturn(List.of("A plain sword"));
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(Mockito.mock(PersistentDataContainer.class));

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertFalse(filter.isProtected(stack));
    }

    // -----------------------------------------------------------------------
    // PDC keys
    // -----------------------------------------------------------------------

    @Test
    void pdcKeyProtectsItemWhenPresent() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("pdc-keys", List.of("myplugin:bound_item"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, Logger.getLogger("test"), "test");
        assertTrue(filter.isEnabled());

        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("myplugin", "bound_item");

        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);
        Mockito.when(pdc.has(key)).thenReturn(true);

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(false);
        Mockito.when(meta.getLore()).thenReturn(null);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertTrue(filter.isProtected(stack));
    }

    @Test
    void pdcKeyDoesNotProtectItemWhenAbsent() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("pdc-keys", List.of("myplugin:bound_item"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, null, "test");

        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("myplugin", "bound_item");

        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);
        Mockito.when(pdc.has(key)).thenReturn(false);

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(false);
        Mockito.when(meta.getLore()).thenReturn(null);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertFalse(filter.isProtected(stack));
    }

    // -----------------------------------------------------------------------
    // Invalid config entries — should not throw, just warn
    // -----------------------------------------------------------------------

    @Test
    void invalidPdcKeyFormatIsSkipped() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("pdc-keys", List.of("not-a-valid-key-no-colon"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, Logger.getLogger("test"), "test");
        // Should load as disabled (no valid rules)
        assertFalse(filter.isEnabled());
    }

    @Test
    void invalidCmdValueIsSkipped() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection s = cfg.createSection("im");
        s.set("enabled", true);
        s.set("custom-model-data", List.of("not-a-number", "500"));

        ItemMetadataFilter filter = ItemMetadataFilter.parse(s, Logger.getLogger("test"), "test");
        assertTrue(filter.isEnabled(), "Valid entry '500' should still produce an active filter");

        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.hasCustomModelData()).thenReturn(true);
        Mockito.when(meta.getCustomModelData()).thenReturn(500);
        Mockito.when(meta.getLore()).thenReturn(null);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(Mockito.mock(PersistentDataContainer.class));

        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);

        assertTrue(filter.isProtected(stack));
    }
}
