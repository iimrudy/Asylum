package eu.asylum.lobby.gui;

import eu.asylum.core.gui.GuiItem;
import eu.asylum.core.gui.GuiListener;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ServerSelectorGUI extends GuiListener {

    public ServerSelectorGUI() {
        super(54, MiniMessage.get().parse("<gradient:#5e4fa2:#f79459:red>SERVER SELECTOR | CLICK TO JOIN</gradient>"));

        this.getGuiItemList().add(new GuiItem(5, this::createCompass, this::onClickCompass));
    }

    private void onClickCompass(Player player, ItemStack itemStack, Inventory inventory) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    public ItemStack createCompass(Player p) {
        return new ItemStack(Material.COMPASS);
    }

}
