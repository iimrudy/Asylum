package eu.asylum.lobby.gui;

import eu.asylum.core.Teleporter;
import eu.asylum.core.gui.GuiItem;
import eu.asylum.core.gui.SimpleGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import static eu.asylum.core.item.ItemBuilder.builder;
import static net.kyori.adventure.text.minimessage.MiniMessage.get;

public class ServerSelectorGUI extends SimpleGui {

    public ServerSelectorGUI(JavaPlugin plugin) {
        super(54, MiniMessage.get().parse("<gradient:#5e4fa2:#f79459:red>SERVER SELECTOR | CLICK TO JOIN</gradient>"), plugin);

        this.getGuiItemList().add(new GuiItem(5, this::createCompass, this::onClickCompass));
    }

    private void onClickCompass(Player player, ItemStack itemStack, Inventory inventory) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
        Teleporter.joinQueueLobby(player);
    }

    public ItemStack createCompass(Player p) {
        return builder().setName(get().parse("<color:#5e4fa2>LOBBY QUEUE </color>")).setMaterial(Material.COMPASS).build();
    }

}
