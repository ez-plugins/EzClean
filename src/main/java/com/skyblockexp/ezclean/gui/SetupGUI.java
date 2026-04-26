package com.skyblockexp.ezclean.gui;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.Registry;
import com.skyblockexp.ezclean.scheduler.EntityCleanupScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inventory-based setup GUI that lets admins configure EzClean cleaner profiles
 * without editing YAML files manually.
 *
 * <p><b>Screens</b></p>
 * <ul>
 *   <li><b>List screen</b> — shows every loaded cleaner profile; admins can open one for
 *       editing or create a fresh profile.</li>
 *   <li><b>Editor screen</b> — shows all configurable toggles/values for a single profile.
 *       Changes are held in memory until the admin clicks "Save &amp; Reload".</li>
 * </ul>
 */
public final class SetupGUI {

    // ── Slot constants — Editor screen (6 rows = 54 slots) ──────────────
    private static final int SLOT_BACK          = 0;
    private static final int SLOT_HEADER        = 4;
    private static final int SLOT_HOSTILE       = 9;
    private static final int SLOT_PASSIVE       = 10;
    private static final int SLOT_VILLAGERS     = 11;
    private static final int SLOT_VEHICLES      = 12;
    private static final int SLOT_DROPPED       = 13;
    private static final int SLOT_PROJECTILES   = 14;
    private static final int SLOT_XP_ORBS       = 15;
    private static final int SLOT_FALLING       = 16;
    private static final int SLOT_TNT           = 17;
    private static final int SLOT_AEC           = 18;
    private static final int SLOT_INTERVAL      = 20;
    private static final int SLOT_MIN_PLAYERS   = 22;
    private static final int SLOT_PILE          = 24;
    private static final int SLOT_WARNING       = 26;
    private static final int SLOT_BROADCAST     = 27;
    private static final int SLOT_CANCEL        = 29;
    private static final int SLOT_ASYNC         = 31;
    private static final int SLOT_CHUNK_CAPS    = 33;
    private static final int SLOT_MERGING       = 35;
    private static final int SLOT_SAVE          = 45;
    private static final int SLOT_DISCARD       = 53;

    // ── Slot constant — List screen (4 rows = 36 slots) ─────────────────
    private static final int LIST_SLOT_NEW      = 27;
    private static final int LIST_SLOT_CLOSE    = 35;
    /** Maximum cleaner profiles displayed on the list screen before truncation. */
    private static final int LIST_MAX_CLEANERS  = 27;

    private final EzCleanPlugin plugin;
    private final Logger logger;
    private final Map<UUID, SetupSession> sessions = new HashMap<>();

    public SetupGUI(EzCleanPlugin plugin) {
        this.plugin = plugin;
        this.logger  = plugin.getLogger();
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Opens the cleaner list screen for the given player.
     * Creates a new session if one does not already exist.
     */
    public void openList(Player player) {
        SetupSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new SetupSession());
        session.setScreen(SetupSession.Screen.LIST);

        List<String> ids = getCleanerIds();

        SetupHolder holder = new SetupHolder();
        Inventory inv = Bukkit.createInventory(holder, 36,
                Component.text("[EzClean] ")
                        .color(NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.BOLD, true)
                        .append(Component.text("Setup — Cleaners")
                                .color(NamedTextColor.GRAY)
                                .decoration(TextDecoration.BOLD, false)));
        holder.bind(inv);

        // Fill first 27 slots with cleaner profile items
        int max = Math.min(ids.size(), LIST_MAX_CLEANERS);
        for (int i = 0; i < max; i++) {
            inv.setItem(i, buildCleanerListItem(ids.get(i)));
        }

        // Navigation row (row 3, slots 27–35)
        for (int s = LIST_SLOT_NEW + 1; s < LIST_SLOT_CLOSE; s++) {
            inv.setItem(s, buildGlassPane());
        }
        inv.setItem(LIST_SLOT_NEW, buildCreateCleanerItem());
        inv.setItem(LIST_SLOT_CLOSE, buildCloseItem());

        player.openInventory(inv);
    }

    /**
     * Opens the editor screen for the given cleaner ID.
     * Loads the cleaner's YAML file as a snapshot for the session.
     */
    public void openEditor(Player player, String cleanerId) {
        File cleanerFile = cleanerFile(cleanerId);
        SetupSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new SetupSession());
        session.setScreen(SetupSession.Screen.EDITOR);
        session.setEditingCleaner(cleanerId);
        session.loadSnapshot(YamlConfiguration.loadConfiguration(cleanerFile));

        renderEditor(player, session);
    }

    /**
     * Handles an inventory click inside a SetupHolder-owned inventory.
     * Cancels the event to prevent item movement, then dispatches to the appropriate handler.
     */
    public void handleClick(Player player, InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        SetupSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (session.getScreen() == SetupSession.Screen.LIST) {
            handleListClick(player, session, slot, event);
        } else {
            handleEditorClick(player, session, slot, event);
        }
    }

    /**
     * Called when the player closes the GUI. Removes the player's session.
     */
    public void onClose(Player player) {
        sessions.remove(player.getUniqueId());
    }

    // ── List click handler ───────────────────────────────────────────────

    private void handleListClick(Player player, SetupSession session, int slot,
                                 InventoryClickEvent event) {
        if (slot == LIST_SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == LIST_SLOT_NEW) {
            createAndOpenNewCleaner(player);
            return;
        }
        // Slots 0–26: cleaner profile items
        List<String> ids = getCleanerIds();
        if (slot < ids.size()) {
            openEditor(player, ids.get(slot));
        }
    }

    // ── Editor click handler ─────────────────────────────────────────────

    private void handleEditorClick(Player player, SetupSession session, int slot,
                                   InventoryClickEvent event) {
        boolean right  = event.isRightClick();
        boolean shift  = event.isShiftClick();

        switch (slot) {
            case SLOT_BACK -> openList(player);
            case SLOT_DISCARD -> player.closeInventory();
            case SLOT_SAVE -> saveAndReload(player, session);

            // Removal toggles
            case SLOT_HOSTILE     -> toggleAndRefresh(player, session, "remove.hostile-mobs",   true);
            case SLOT_PASSIVE     -> toggleAndRefresh(player, session, "remove.passive-mobs",   false);
            case SLOT_VILLAGERS   -> toggleAndRefresh(player, session, "remove.villagers",       false);
            case SLOT_VEHICLES    -> toggleAndRefresh(player, session, "remove.vehicles",        false);
            case SLOT_DROPPED     -> toggleAndRefresh(player, session, "remove.dropped-items",  true);
            case SLOT_PROJECTILES -> toggleAndRefresh(player, session, "remove.projectiles",    true);
            case SLOT_XP_ORBS     -> toggleAndRefresh(player, session, "remove.experience-orbs", true);
            case SLOT_FALLING     -> toggleAndRefresh(player, session, "remove.falling-blocks", true);
            case SLOT_TNT         -> toggleAndRefresh(player, session, "remove.primed-tnt",     true);
            case SLOT_AEC         -> toggleAndRefresh(player, session, "remove.area-effect-clouds", true);

            // Feature toggles
            case SLOT_PILE        -> toggleAndRefresh(player, session, "pile-detection.enabled", true);
            case SLOT_WARNING     -> toggleAndRefresh(player, session, "warning.enabled",        false);
            case SLOT_BROADCAST   -> toggleAndRefresh(player, session, "broadcast.start.enabled", true);
            case SLOT_CANCEL      -> toggleAndRefresh(player, session, "cancel.enabled",         true);
            case SLOT_ASYNC       -> toggleAndRefresh(player, session, "async-removal",          false);
            case SLOT_CHUNK_CAPS  -> toggleAndRefresh(player, session, "chunk-caps.enabled",     false);
            case SLOT_MERGING     -> toggleAndRefresh(player, session, "merging.enabled",        false);

            // Numeric controls
            case SLOT_INTERVAL -> {
                int step  = shift ? 10 : 1;
                int delta = right ? step : -step;
                int current = session.getInt("interval-minutes", 60);
                int next = Math.max(1, current + delta);
                session.set("interval-minutes", next);
                refreshEditorSlot(player, session, SLOT_INTERVAL);
            }
            case SLOT_MIN_PLAYERS -> {
                int step  = shift ? 10 : 1;
                int delta = right ? step : -step;
                int current = session.getInt("min-players", 0);
                int next = Math.max(0, current + delta);
                session.set("min-players", next);
                refreshEditorSlot(player, session, SLOT_MIN_PLAYERS);
            }
            default -> { /* no-op for glass panes or other non-interactive slots */ }
        }
    }

    // ── Session helpers ──────────────────────────────────────────────────

    private void toggleAndRefresh(Player player, SetupSession session, String path, boolean def) {
        boolean current = session.getBoolean(path, def);
        session.set(path, !current);
        // Find the slot that maps to this path and refresh it
        int slot = pathToSlot(path);
        if (slot >= 0) refreshEditorSlot(player, session, slot);
    }

    private static int pathToSlot(String path) {
        return switch (path) {
            case "remove.hostile-mobs"        -> SLOT_HOSTILE;
            case "remove.passive-mobs"        -> SLOT_PASSIVE;
            case "remove.villagers"           -> SLOT_VILLAGERS;
            case "remove.vehicles"            -> SLOT_VEHICLES;
            case "remove.dropped-items"       -> SLOT_DROPPED;
            case "remove.projectiles"         -> SLOT_PROJECTILES;
            case "remove.experience-orbs"     -> SLOT_XP_ORBS;
            case "remove.falling-blocks"      -> SLOT_FALLING;
            case "remove.primed-tnt"          -> SLOT_TNT;
            case "remove.area-effect-clouds"  -> SLOT_AEC;
            case "pile-detection.enabled"     -> SLOT_PILE;
            case "warning.enabled"            -> SLOT_WARNING;
            case "broadcast.start.enabled"    -> SLOT_BROADCAST;
            case "cancel.enabled"             -> SLOT_CANCEL;
            case "async-removal"              -> SLOT_ASYNC;
            case "chunk-caps.enabled"         -> SLOT_CHUNK_CAPS;
            case "merging.enabled"            -> SLOT_MERGING;
            default                           -> -1;
        };
    }

    /** Refreshes a single slot in the player's currently open editor inventory. */
    private void refreshEditorSlot(Player player, SetupSession session, int slot) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof SetupHolder)) return;
        inv.setItem(slot, buildEditorItem(session, slot));
    }

    // ── Save ─────────────────────────────────────────────────────────────

    private void saveAndReload(Player player, SetupSession session) {
        String cleanerId = session.getEditingCleaner();
        File cleanerFile = cleanerFile(cleanerId);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(cleanerFile);
        for (Map.Entry<String, Object> entry : session.getPendingChanges().entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }

        try {
            yaml.save(cleanerFile);
        } catch (IOException ex) {
            logger.log(Level.WARNING,
                    "SetupGUI: failed to save cleaner file for '" + cleanerId + "'", ex);
            player.sendMessage(Component.text("[EzClean] ")
                    .color(NamedTextColor.DARK_AQUA)
                    .append(Component.text("Failed to save changes. Check server logs.")
                            .color(NamedTextColor.RED)));
            return;
        }

        plugin.reloadPluginConfiguration();

        player.closeInventory();
        player.sendMessage(Component.text("[EzClean] ")
                .color(NamedTextColor.DARK_AQUA)
                .append(Component.text("Cleaner ")
                        .color(NamedTextColor.GREEN))
                .append(Component.text("'" + cleanerId + "'")
                        .color(NamedTextColor.AQUA))
                .append(Component.text(" saved and reloaded.")
                        .color(NamedTextColor.GREEN)));
    }

    // ── Create new cleaner ────────────────────────────────────────────────

    private void createAndOpenNewCleaner(Player player) {
        File cleanersDir = new File(plugin.getDataFolder(), "cleaners");
        if (!cleanersDir.exists()) {
            cleanersDir.mkdirs();
        }

        // Find a unique name: "new-cleaner.yml", "new-cleaner-1.yml", …
        String baseName = "new-cleaner";
        File target = new File(cleanersDir, baseName + ".yml");
        int suffix = 1;
        while (target.exists()) {
            target = new File(cleanersDir, baseName + "-" + suffix + ".yml");
            suffix++;
        }

        // Copy defaults
        String resourcePath = "cleaners/default.yml";
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                player.sendMessage(Component.text("[EzClean] ")
                        .color(NamedTextColor.DARK_AQUA)
                        .append(Component.text("Could not find the default cleaner template.")
                                .color(NamedTextColor.RED)));
                return;
            }
            Files.copy(in, target.toPath());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "SetupGUI: failed to create new cleaner file", ex);
            player.sendMessage(Component.text("[EzClean] ")
                    .color(NamedTextColor.DARK_AQUA)
                    .append(Component.text("Failed to create cleaner file. Check server logs.")
                            .color(NamedTextColor.RED)));
            return;
        }

        String cleanerId = target.getName().replaceFirst("\\.[^.]+$", "");
        player.sendMessage(Component.text("[EzClean] ")
                .color(NamedTextColor.DARK_AQUA)
                .append(Component.text("Created new cleaner ")
                        .color(NamedTextColor.GREEN))
                .append(Component.text("'" + cleanerId + "'")
                        .color(NamedTextColor.AQUA))
                .append(Component.text(". Edit the settings below and click Save.")
                        .color(NamedTextColor.GREEN)));

        openEditor(player, cleanerId);
    }

    // ── Inventory builder ────────────────────────────────────────────────

    private void renderEditor(Player player, SetupSession session) {
        String id = session.getEditingCleaner();
        SetupHolder holder = new SetupHolder();
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("[EzClean] ")
                        .color(NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.BOLD, true)
                        .append(Component.text("Editing: ")
                                .color(NamedTextColor.GRAY)
                                .decoration(TextDecoration.BOLD, false))
                        .append(Component.text(id)
                                .color(NamedTextColor.AQUA)
                                .decoration(TextDecoration.BOLD, false)));
        holder.bind(inv);

        ItemStack glass = buildGlassPane();

        // Row 0: nav bar
        inv.setItem(SLOT_BACK,   buildBackItem());
        inv.setItem(1, glass); inv.setItem(2, glass); inv.setItem(3, glass);
        inv.setItem(SLOT_HEADER, buildHeaderItem(id));
        inv.setItem(5, glass); inv.setItem(6, glass); inv.setItem(7, glass); inv.setItem(8, glass);

        // Row 1: removal toggles (9–17)
        for (int s = SLOT_HOSTILE; s <= SLOT_TNT; s++) {
            inv.setItem(s, buildEditorItem(session, s));
        }

        // Row 2: area-effect-clouds + interval + min-players + pile + warning
        inv.setItem(SLOT_AEC,         buildEditorItem(session, SLOT_AEC));
        inv.setItem(19, glass);
        inv.setItem(SLOT_INTERVAL,    buildEditorItem(session, SLOT_INTERVAL));
        inv.setItem(21, glass);
        inv.setItem(SLOT_MIN_PLAYERS, buildEditorItem(session, SLOT_MIN_PLAYERS));
        inv.setItem(23, glass);
        inv.setItem(SLOT_PILE,        buildEditorItem(session, SLOT_PILE));
        inv.setItem(25, glass);
        inv.setItem(SLOT_WARNING,     buildEditorItem(session, SLOT_WARNING));

        // Row 3: feature toggles
        inv.setItem(SLOT_BROADCAST,  buildEditorItem(session, SLOT_BROADCAST));
        inv.setItem(28, glass);
        inv.setItem(SLOT_CANCEL,     buildEditorItem(session, SLOT_CANCEL));
        inv.setItem(30, glass);
        inv.setItem(SLOT_ASYNC,      buildEditorItem(session, SLOT_ASYNC));
        inv.setItem(32, glass);
        inv.setItem(SLOT_CHUNK_CAPS, buildEditorItem(session, SLOT_CHUNK_CAPS));
        inv.setItem(34, glass);
        inv.setItem(SLOT_MERGING,    buildEditorItem(session, SLOT_MERGING));

        // Row 4: separator
        for (int s = 36; s <= 44; s++) inv.setItem(s, glass);

        // Row 5: action bar
        inv.setItem(SLOT_SAVE,    buildSaveItem());
        for (int s = 46; s <= 52; s++) inv.setItem(s, glass);
        inv.setItem(SLOT_DISCARD, buildDiscardItem());

        player.openInventory(inv);
    }

    /**
     * Builds the appropriate ItemStack for an editor slot, given the current session state.
     * Returns null for glass-pane slots (they are set directly in {@link #renderEditor}).
     */
    private ItemStack buildEditorItem(SetupSession session, int slot) {
        return switch (slot) {
            case SLOT_HOSTILE     -> buildToggle(Material.ZOMBIE_SPAWN_EGG,  "Hostile Mobs",
                    "Removes monsters (zombies, creepers, etc.)",
                    session.getBoolean("remove.hostile-mobs", true));
            case SLOT_PASSIVE     -> buildToggle(Material.COW_SPAWN_EGG,     "Passive Mobs",
                    "Removes farm animals, golems, aquatic mobs",
                    session.getBoolean("remove.passive-mobs", false));
            case SLOT_VILLAGERS   -> buildToggle(Material.VILLAGER_SPAWN_EGG, "Villagers",
                    "Removes villagers and wandering traders",
                    session.getBoolean("remove.villagers", false));
            case SLOT_VEHICLES    -> buildToggle(Material.MINECART,          "Vehicles",
                    "Removes minecarts and boats",
                    session.getBoolean("remove.vehicles", false));
            case SLOT_DROPPED     -> buildToggle(Material.IRON_NUGGET,       "Dropped Items",
                    "Removes dropped item entities on the ground",
                    session.getBoolean("remove.dropped-items", true));
            case SLOT_PROJECTILES -> buildToggle(Material.BOW,               "Projectiles",
                    "Removes arrows, fireballs, snowballs, etc.",
                    session.getBoolean("remove.projectiles", true));
            case SLOT_XP_ORBS     -> buildToggle(Material.EXPERIENCE_BOTTLE, "Experience Orbs",
                    "Removes lingering XP orb entities",
                    session.getBoolean("remove.experience-orbs", true));
            case SLOT_FALLING     -> buildToggle(Material.SAND,              "Falling Blocks",
                    "Removes falling-block entities (sand, gravel)",
                    session.getBoolean("remove.falling-blocks", true));
            case SLOT_TNT         -> buildToggle(Material.TNT,               "Primed TNT",
                    "Removes primed (lit) TNT entities",
                    session.getBoolean("remove.primed-tnt", true));
            case SLOT_AEC         -> buildToggle(Material.GLASS_BOTTLE,      "Area Effect Clouds",
                    "Removes lingering potion clouds",
                    session.getBoolean("remove.area-effect-clouds", true));
            case SLOT_PILE        -> buildToggle(Material.CAULDRON,          "Pile Detection",
                    "Culls excessive stacks of entities on one block",
                    session.getBoolean("pile-detection.enabled", true));
            case SLOT_WARNING     -> buildToggle(Material.BELL,              "Pre-cleanup Warning",
                    "Warn players before cleanup runs",
                    session.getBoolean("warning.enabled", false));
            case SLOT_BROADCAST   -> buildToggle(Material.PAPER,             "Start Broadcast",
                    "Announce in chat when cleanup starts",
                    session.getBoolean("broadcast.start.enabled", true));
            case SLOT_CANCEL      -> buildToggle(Material.GOLD_INGOT,        "Pay-to-Cancel",
                    "Allow players to pay Vault currency to cancel",
                    session.getBoolean("cancel.enabled", true));
            case SLOT_ASYNC       -> buildToggle(Material.PISTON,            "Async Removal",
                    "Spread entity removal across multiple ticks",
                    session.getBoolean("async-removal", false));
            case SLOT_CHUNK_CAPS  -> buildToggle(Material.STONE,             "Chunk Caps",
                    "Remove excess entities when a chunk is over-limit",
                    session.getBoolean("chunk-caps.enabled", false));
            case SLOT_MERGING     -> buildToggle(Material.MAGMA_CREAM,       "Entity Merging",
                    "Merge nearby duplicate items/XP orbs instead of deleting",
                    session.getBoolean("merging.enabled", false));
            case SLOT_INTERVAL    -> buildIntervalItem(session.getInt("interval-minutes", 60));
            case SLOT_MIN_PLAYERS -> buildMinPlayersItem(session.getInt("min-players", 0));
            default               -> null;
        };
    }

    // ── Item builders ────────────────────────────────────────────────────

    /**
     * Builds a toggle item. The display name is green (✔) when enabled, red (✘) when disabled,
     * and italic decoration is suppressed to match MC's inventory convention.
     */
    private static ItemStack buildToggle(Material mat, String label, String description, boolean enabled) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Component prefix = enabled
                ? Component.text("✔ ").color(NamedTextColor.GREEN)
                : Component.text("✘ ").color(NamedTextColor.RED);
        meta.displayName(prefix
                .append(Component.text(label).color(NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("  " + description)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("  Status: ")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(enabled ? "Enabled" : "Disabled")
                        .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.text("  Left-click to " + (enabled ? "disable" : "enable"))
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildIntervalItem(int minutes) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Cleanup Interval")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("  Every: ")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(minutes + " minutes").color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("  Left-click:       −1 minute")
                        .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("  Right-click:      +1 minute")
                        .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("  Shift+Left:       −10 minutes")
                        .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("  Shift+Right:      +10 minutes")
                        .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildMinPlayersItem(int minPlayers) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Minimum Online Players")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("  Required online: ")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(minPlayers == 0
                                ? "any (always runs)" : minPlayers + " players")
                                .color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("  Left-click:  −1 player")
                        .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("  Right-click: +1 player")
                        .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildCleanerListItem(String cleanerId) {
        File cleanerFile = new File(
                Bukkit.getPluginManager().getPlugin("EzClean").getDataFolder(),
                "cleaners/" + cleanerId + ".yml");

        int interval = 60;
        String worlds = "*";
        if (cleanerFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(cleanerFile);
            interval = cfg.getInt("interval-minutes", 60);
            List<String> worldList = cfg.getStringList("worlds");
            if (!worldList.isEmpty()) {
                worlds = String.join(", ", worldList);
            }
        }

        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(cleanerId)
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("  Interval: ")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("every " + interval + " min").color(NamedTextColor.WHITE)),
                Component.text("  Worlds:   ")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(worlds).color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("  Click to edit")
                        .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildHeaderItem(String cleanerId) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Cleaner: ")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(cleanerId).color(NamedTextColor.AQUA)));
        meta.lore(Arrays.asList(
                Component.text("  Adjust settings below, then")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  click Save & Reload to apply.")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("  Note: worlds & advanced options")
                        .color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  are edited directly in the YAML.")
                        .color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildCreateCleanerItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("+ Create New Cleaner")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Collections.singletonList(
                Component.text("  Creates a new cleaner profile from defaults.")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("⬅ Back to Cleaners")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildSaveItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Save & Reload")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("  Saves changes to the YAML file")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("  and triggers a hot-reload.")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildDiscardItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Close without Saving")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Close")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildGlassPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    // ── Utilities ────────────────────────────────────────────────────────

    private static List<String> getCleanerIds() {
        EntityCleanupScheduler scheduler = Registry.getCleanupScheduler();
        if (scheduler == null) return Collections.emptyList();
        return scheduler.getCleanerIds();
    }

    private File cleanerFile(String cleanerId) {
        return new File(plugin.getDataFolder(), "cleaners/" + cleanerId + ".yml");
    }
}
