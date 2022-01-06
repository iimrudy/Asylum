package eu.asylum.core.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGui implements Listener {

    protected final int size;
    protected Component title;

    public AbstractGui(int size, Component title) {
        this.size = size;
        this.title = title;
    }


    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(this.title)) return;

        event.setCancelled(true);

        final ItemStack clickedItem = event.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        onItemClick(player, clickedItem, event.getView().getTopInventory());

    }

    protected abstract void onItemClick(Player player, ItemStack clickedItem, org.bukkit.inventory.Inventory inventory);

    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getView().title().equals(this.title)) {
            e.setCancelled(true);
        }
    }

    public abstract void openInventory(Player player);

}
