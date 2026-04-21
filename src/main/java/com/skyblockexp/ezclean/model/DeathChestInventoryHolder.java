package com.skyblockexp.ezclean.model;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class DeathChestInventoryHolder implements InventoryHolder {
    private final DeathChest deathChest;
    private Inventory inventory;

    public DeathChestInventoryHolder(DeathChest deathChest) {
        this.deathChest = deathChest;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public DeathChest getDeathChest() {
        return deathChest;
    }
}
