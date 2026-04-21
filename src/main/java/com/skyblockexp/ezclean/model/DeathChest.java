package com.skyblockexp.ezclean.model;

import com.skyblockexp.ezclean.manager.DeathChestManager;
import com.skyblockexp.ezclean.util.FoliaScheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class DeathChest {
    // Hologram and particle effect fields
    private org.bukkit.entity.ArmorStand hologramStand;
    private Runnable cancelParticleTask;

    private final Location location;
    private final List<ItemStack> drops;
    private final Duration despawnAfter;
    private Inventory inventory;
    private DeathChestInventoryHolder holder;
    private Runnable cancelDespawnTask;
    private final java.util.UUID owner;
    private final long protectionEndMillis;
    private final DeathChestManager manager;

    public DeathChest(DeathChestManager manager, Location location, List<ItemStack> drops, Duration despawnAfter) {
        this.manager = manager;
        this.location = location;
        this.drops = drops;
        this.despawnAfter = despawnAfter;
        this.owner = null;
        this.protectionEndMillis = 0L;
    }

    public DeathChest(DeathChestManager manager, Location location, List<ItemStack> drops, Duration despawnAfter, java.util.UUID owner, long protectionEndMillis) {
        this.manager = manager;
        this.location = location;
        this.drops = drops;
        this.despawnAfter = despawnAfter;
        this.owner = owner;
        this.protectionEndMillis = protectionEndMillis;
    }

    public Location getLocation() {
        return location;
    }

    public void place() {
        holder = new DeathChestInventoryHolder(this);
        inventory = manager.createInventory(holder);
        holder.setInventory(inventory);
        for (ItemStack item : drops) {
            if (item == null) {
                continue;
            }
            Map<Integer, ItemStack> overflow = inventory.addItem(item);
            if (!overflow.isEmpty() && location.getWorld() != null) {
                Location dropLocation = location.clone().add(0.5, 0.5, 0.5);
                for (ItemStack leftover : overflow.values()) {
                    if (leftover == null || leftover.getType().isAir()) {
                        continue;
                    }
                    location.getWorld().dropItemNaturally(dropLocation, leftover);
                }
            }
        }
        scheduleDespawn();
        // Hologram
        if (manager.getSettings().isHologramEnabled() && location.getWorld() != null) {
            hologramStand = location.getWorld().spawn(location.clone().add(0.5, 1.2, 0.5), org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setMarker(true);
                stand.setCustomNameVisible(true);
                try {
                    // Try Adventure API if available
                    Class<?> bcsClass = Class.forName("net.kyori.adventure.platform.bukkit.BukkitComponentSerializer");
                    Object legacy = bcsClass.getMethod("legacy").invoke(null);
                    Object component = legacy.getClass().getMethod("deserialize", String.class).invoke(legacy, manager.getSettings().getHologramText());
                    // Try toString, fallback to plain string
                    String name = component != null ? component.toString() : manager.getSettings().getHologramText();
                    stand.setCustomName(name);
                } catch (Exception e) {
                    // Fallback to plain string
                    stand.setCustomName(manager.getSettings().getHologramText());
                }
                stand.setGravity(false);
                stand.setSmall(true);
                stand.setInvulnerable(true);
                stand.setPersistent(false);
            });
        }
        // Particles
        if (manager.getSettings().isParticlesEnabled() && location.getWorld() != null) {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(manager.getSettings().getParticleType());
            cancelParticleTask = FoliaScheduler.runGlobalTimer(manager.getPlugin(), () -> {
                location.getWorld().spawnParticle(particle, location.clone().add(0.5, 1.1, 0.5), 6, 0.2, 0.1, 0.2, 0.01);
            }, 0L, 20L);
        }
    }

    public void open(Player player) {
        if (inventory == null) {
            return;
        }
        // Loot protection logic
        if (manager.getSettings().isLootProtectionEnabled() && owner != null && protectionEndMillis > 0) {
            long now = System.currentTimeMillis();
            boolean isOwner = player.getUniqueId().equals(owner);
            boolean bypass = player.hasPermission("ezclean.deathchest.protection.bypass");
            if (!isOwner && !bypass && now < protectionEndMillis) {
                player.sendMessage(org.bukkit.ChatColor.RED + "This chest is protected. Only the owner can open it for " + manager.getSettings().getLootProtectionMinutes() + " minutes after death.");
                return;
            }
        }
        player.openInventory(inventory);
    }


    public void scheduleDespawn() {
        if (despawnAfter == null || despawnAfter.isZero() || despawnAfter.isNegative()) {
            return;
        }
        long ticks = despawnAfter.toMillis() / 50L;
        if (ticks <= 0) {
            return;
        }
        cancelDespawnTask = FoliaScheduler.runGlobalLater(manager.getPlugin(), () -> {
            if (inventory == null) {
                return;
            }
            manager.removeChest(this, RemovalReason.DESPAWN);
        }, ticks);
    }

    public void destroy(RemovalReason reason) {
        if (cancelDespawnTask != null) {
            cancelDespawnTask.run();
            cancelDespawnTask = null;
        }

        // Determine if we should remove the chest block
        boolean shouldRemoveBlock = true;
        boolean isOffline = false;
        if (owner != null) {
            org.bukkit.OfflinePlayer offline = manager.getPlugin().getServer().getOfflinePlayer(owner);
            isOffline = !offline.isOnline();
        }

        // Check if we should keep the chest for offline owner
        if (reason == RemovalReason.DESPAWN || reason == RemovalReason.BROKEN
            || reason == RemovalReason.DISABLED || reason == RemovalReason.PLUGIN_DISABLE) {
            if (isOffline && "keep".equalsIgnoreCase(manager.getSettings().getOfflineOwnerHandling())) {
                shouldRemoveBlock = false; // Keep the chest block for offline owner
            }
        }

        // Remove chest block if needed
        if (shouldRemoveBlock) {
            Block block = location.getBlock();
            if (block.getType() == Material.CHEST) {
                block.setType(Material.AIR);
            }
        }

        if (inventory != null) {
            List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
            for (HumanEntity viewer : viewers) {
                viewer.closeInventory();
            }
                if (reason == RemovalReason.DESPAWN || reason == RemovalReason.BROKEN
                    || reason == RemovalReason.DISABLED || reason == RemovalReason.PLUGIN_DISABLE) {
                if (isOffline) {
                    String mode = manager.getSettings().getOfflineOwnerHandling();
                    if ("drop".equalsIgnoreCase(mode)) {
                        dropContents();
                    } else if ("keep".equalsIgnoreCase(mode)) {
                        // Do not clear inventory, leave chest for later
                        // Visual effects will still be cleaned up below
                    } // else remove: do nothing (delete with no drops)
                } else {
                    dropContents();
                }
            }
            // Only clear inventory if we're not keeping it for offline owner
            if (shouldRemoveBlock) {
                inventory.clear();
                inventory = null;
                holder = null;
            }
        }

        // Always clean up visual effects regardless of offline handling mode
        // Remove hologram
        if (hologramStand != null && !hologramStand.isDead()) {
            hologramStand.remove();
            hologramStand = null;
        }
        // Cancel particle task
        if (cancelParticleTask != null) {
            cancelParticleTask.run();
            cancelParticleTask = null;
        }
    }

    public void dropContents() {
        if (inventory == null || location.getWorld() == null) {
            return;
        }
        Location dropLocation = location.clone().add(0.5, 0.5, 0.5);
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            location.getWorld().dropItemNaturally(dropLocation, item);
        }
    }

    public DeathChestInventoryHolder getHolder() {
        return holder;
    }
}
