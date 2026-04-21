package com.skyblockexp.ezclean.manager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
// Import split classes
import com.skyblockexp.ezclean.model.DeathChest;
import com.skyblockexp.ezclean.model.DeathChestInventoryHolder;
import com.skyblockexp.ezclean.model.RemovalReason;
import com.skyblockexp.ezclean.model.DeathChestSettings;
import com.skyblockexp.ezclean.EzCleanPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Manages spawning and lifecycle of death chests when players die.
 */
public final class DeathChestManager implements Listener {

    public DeathChestSettings getSettings() {
        return settings;
    }

    public EzCleanPlugin getPlugin() {
        return plugin;
    }

    private static final int INVENTORY_SIZE = 54;

    private static final Method BUKKIT_CREATE_INVENTORY_COMPONENT;
    private static final Method BUKKIT_CREATE_INVENTORY_STRING;

    static {
        Method component = null;
        Method string = null;
        try {
            component = Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class,
                    net.kyori.adventure.text.Component.class);
        } catch (NoSuchMethodException ignored) {
            // Older versions may not support Adventure titles; fall back to strings.
        }
        try {
            string = Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class, String.class);
        } catch (NoSuchMethodException ignored) {
            // All supported versions should provide this overload, but guard just in case.
        }
        BUKKIT_CREATE_INVENTORY_COMPONENT = component;
        BUKKIT_CREATE_INVENTORY_STRING = string;
    }

    private final EzCleanPlugin plugin;
    private final Map<Location, DeathChest> chestsByLocation = new HashMap<>();
    // Track active death chests per player UUID
    private final Map<java.util.UUID, List<DeathChest>> chestsByPlayer = new HashMap<>();
    private DeathChestSettings settings;
    private boolean registered;

    public DeathChestManager(EzCleanPlugin plugin, DeathChestSettings initialSettings) {
        this.plugin = plugin;
        this.settings = Objects.requireNonNull(initialSettings, "initialSettings");
        if (initialSettings.isEnabled()) {
            register();
        }
    }

    public void applySettings(DeathChestSettings newSettings) {
        Objects.requireNonNull(newSettings, "newSettings");
        boolean wasEnabled = settings != null && settings.isEnabled();
        settings = newSettings;
        if (!wasEnabled && newSettings.isEnabled()) {
            register();
        } else if (wasEnabled && !newSettings.isEnabled()) {
            unregister();
            removeAllChests(RemovalReason.DISABLED);
        }
    }

    public void shutdown() {
        unregister();
        removeAllChests(RemovalReason.PLUGIN_DISABLE);
    }

    private void register() {
        if (registered) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    private void unregister() {
        if (!registered) {
            return;
        }
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (event.getDrops().isEmpty()) {
            return;
        }
        Location placement = findChestLocation(event.getEntity().getLocation());
        if (placement == null) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();

        spawnDeathChest(event.getEntity(), placement, drops);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestInteract(PlayerInteractEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!event.hasBlock()) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        DeathChest chest = chestsByLocation.get(block.getLocation().toBlockLocation());
        if (chest == null) {
            return;
        }
        event.setCancelled(true);
        chest.open(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeathChestBreak(BlockBreakEvent event) {
        if (!isEnabled()) {
            return;
        }
        DeathChest chest = chestsByLocation.get(event.getBlock().getLocation().toBlockLocation());
        if (chest == null) {
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
        removeChest(chest, RemovalReason.BROKEN);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathChestInventoryClose(InventoryCloseEvent event) {
        if (!isEnabled()) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof DeathChestInventoryHolder deathChestHolder)) {
            return;
        }
        DeathChest chest = deathChestHolder.getDeathChest();
        if (chest == null) {
            return;
        }
        if (event.getInventory().isEmpty()) {
            removeChest(chest, RemovalReason.EMPTY);
        }
    }

    private boolean isEnabled() {
        return settings != null && settings.isEnabled();
    }

    private void spawnDeathChest(Player player, Location location, List<ItemStack> drops) {
        // Enforce per-player chest limit unless player has bypass permission
        int maxChests = settings.getMaxChestsPerPlayer();
        java.util.UUID uuid = player.getUniqueId();
        if (maxChests > 0 && !player.hasPermission("ezclean.deathchest.limit.bypass")) {
            List<DeathChest> playerChests = chestsByPlayer.getOrDefault(uuid, new ArrayList<>());
            while (playerChests.size() >= maxChests) {
                // Remove oldest chest
                DeathChest oldest = playerChests.remove(0);
                removeChest(oldest, RemovalReason.DESPAWN);
            }
        }

        Block block = location.getBlock();
        block.setType(Material.CHEST);
        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            chest.update(true, false);
        }

        long protectionEndMillis = 0L;
        java.util.UUID owner = null;
        if (settings.isLootProtectionEnabled() && settings.getLootProtectionMinutes() > 0) {
            owner = player.getUniqueId();
            protectionEndMillis = System.currentTimeMillis() + settings.getLootProtectionMinutes() * 60_000L;
        }
        DeathChest deathChest = (owner != null)
            ? new DeathChest(this, location.toBlockLocation(), drops, settings.getDespawnAfter(), owner, protectionEndMillis)
            : new DeathChest(this, location.toBlockLocation(), drops, settings.getDespawnAfter());
        chestsByLocation.put(deathChest.getLocation(), deathChest);
        // Track by player
        chestsByPlayer.computeIfAbsent(uuid, k -> new ArrayList<>()).add(deathChest);
        deathChest.place();
    }

    public void removeChest(DeathChest chest, RemovalReason reason) {
        chestsByLocation.remove(chest.getLocation());
        // Remove from player tracking
        for (List<DeathChest> list : chestsByPlayer.values()) {
            list.remove(chest);
        }
        chest.destroy(reason);
    }

    private void removeAllChests(RemovalReason reason) {
        if (chestsByLocation.isEmpty()) {
            return;
        }
        List<DeathChest> active = new ArrayList<>(chestsByLocation.values());
        chestsByLocation.clear();
        chestsByPlayer.clear();
        for (DeathChest chest : active) {
            chest.destroy(reason);
        }
    }

    private @Nullable Location findChestLocation(Location deathLocation) {
        Location base = deathLocation == null ? null : deathLocation.getBlock().getLocation();
        if (base == null || base.getWorld() == null) {
            return null;
        }
        Location candidate = searchAround(base);
        if (candidate != null) {
            return candidate;
        }
        Location above = base.clone().add(0, 1, 0);
        return searchAround(above);
    }

    private @Nullable Location searchAround(Location origin) {
        Location base = origin.toBlockLocation();
        if (isValidPlacement(base)) {
            return base;
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location test = origin.clone().add(x, y, z).toBlockLocation();
                    if (isValidPlacement(test)) {
                        return test;
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidPlacement(Location location) {
        Block block = location.getBlock();
        if (!block.getType().isAir() || chestsByLocation.containsKey(location)) {
            return false;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        return below.getType().isSolid() && !below.isLiquid();
    }

    public Inventory createInventory(InventoryHolder holder) {
        if (BUKKIT_CREATE_INVENTORY_COMPONENT != null && settings.getInventoryTitle() != null) {
            try {
                return (Inventory) BUKKIT_CREATE_INVENTORY_COMPONENT.invoke(null, holder, INVENTORY_SIZE,
                        settings.getInventoryTitle());
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Fallback to legacy string below.
            }
        }
        if (BUKKIT_CREATE_INVENTORY_STRING != null && settings.getLegacyInventoryTitle() != null) {
            try {
                return (Inventory) BUKKIT_CREATE_INVENTORY_STRING.invoke(null, holder, INVENTORY_SIZE,
                        settings.getLegacyInventoryTitle());
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Fallback to default inventory below.
            }
        }
        return Bukkit.createInventory(holder, INVENTORY_SIZE);
    }


}
