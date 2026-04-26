package com.skyblockexp.ezclean.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listens for inventory interactions and forwards them to {@link SetupGUI}.
 * All clicks inside a SetupHolder-owned inventory are cancelled to prevent
 * item movement, and then delegated to the GUI handler.
 */
public final class SetupGUIListener implements Listener {

    private final SetupGUI gui;

    public SetupGUIListener(SetupGUI gui) {
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof SetupHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        gui.handleClick(player, event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof SetupHolder)) return;

        if (!(event.getPlayer() instanceof Player player)) return;

        gui.onClose(player);
    }
}
