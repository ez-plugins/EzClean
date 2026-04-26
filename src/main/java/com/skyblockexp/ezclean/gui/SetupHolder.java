package com.skyblockexp.ezclean.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker {@link InventoryHolder} for all EzClean setup GUI inventories.
 * Used to identify EzClean-owned inventories in click / close events.
 */
public final class SetupHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void bind(Inventory inventory) {
        this.inventory = inventory;
    }
}
