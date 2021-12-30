package eu.asylum.core.gui;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GuiListener implements Listener {


    private final int size;
    private final List<GuiItem> guiItemList = new ArrayList<>();
    private Component title;

    public GuiListener(int size, Component title) {
        this.size = size;
        this.title = title;
    }

    public void openInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, size, title);
        for (var item : this.guiItemList) {
            inv.setItem(item.getSlot(), item.getItemBuilder().build(player));
        }
        player.openInventory(inv);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(this.title)) return;

        event.setCancelled(true);

        final ItemStack clickedItem = event.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        for (var item : this.guiItemList) {
            if (item.getItemBuilder().build(player).isSimilar(clickedItem)) {
                item.getOnItemClick().onItemClick(player, clickedItem, event.getInventory());
                break;
            }
        }

    }

    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getView().title().equals(this.title)) {
            e.setCancelled(true);
        }
    }

}
