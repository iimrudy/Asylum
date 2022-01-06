package eu.asylum.core.gui;

import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.servers.Server;
import eu.asylum.core.AsylumCore;
import eu.asylum.core.Teleporter;
import eu.asylum.core.events.OnServerAddEvent;
import eu.asylum.core.events.OnServerDeleteEvent;
import eu.asylum.core.events.OnServerUpdateEvent;
import eu.asylum.core.events.OnSyncEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ServerGui extends AbstractGui {


    private final Inventory inventory;
    private final ServerType serverType;

    public ServerGui(ServerType serverType, Component title) {
        super(54, title);
        this.serverType = serverType;
        this.inventory = Bukkit.createInventory(null, this.size, this.title);
        rebuildUI();
    }

    private void rebuildUI() {
        this.inventory.clear();
        inventory.clear();
        for (int i = 0; i < this.size; i++) {
            inventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
        }
        var servers = new ArrayList<>(AsylumCore.getInstance().getAsylumProvider().getRepository().getServers(this.serverType));
        for (Server server : servers) {
            try {
                Integer i = Integer.valueOf(server.getName().split("-")[1]);
                inventory.setItem(i, craftItem(server));
            } catch (Exception e) {
                // ignored exception
            }
        }
    }

    public void openInventory(Player player) {
        player.openInventory(this.inventory);
    }

    @Override
    protected void onItemClick(Player player, ItemStack clickedItem, Inventory inventory) {
        if (clickedItem.getType().name().contains("TERRACOTTA")) {
            String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            Teleporter.connect(player, itemName);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 5, -20);
        }
    }

    public ItemStack craftItem(Server server) {
        var mat = (server.getServerStatus().getOnlinePlayers() >= server.getServerType().getMaxPlayers() / 2 ? Material.ORANGE_TERRACOTTA : Material.GREEN_TERRACOTTA);
        ItemStack itemStack = new ItemStack(mat, 1);

        ItemMeta meta = itemStack.getItemMeta();

        var dn = LegacyComponentSerializer.legacySection().deserialize(ChatColor.YELLOW + StringUtils.capitalize(server.getName().toLowerCase(Locale.ROOT)));

        var empty = LegacyComponentSerializer.legacySection().deserialize("");

        meta.displayName(dn);
        meta.lore(Arrays.asList(empty, LegacyComponentSerializer.legacyAmpersand().deserialize("&7Players &e&l" + server.getServerStatus().getOnlinePlayers() + "&7/&e&l" + server.getServerType().getMaxPlayers()), empty));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @EventHandler
    public void onServerAdd(OnServerAddEvent event) {
        if (event.getServer().getServerType().equals(this.serverType)) {
            try {
                Integer i = Integer.valueOf(event.getServer().getName().split("-")[1]);
                inventory.setItem(i, craftItem(event.getServer()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onServerDelete(OnServerDeleteEvent event) {
        if (event.getServer().getServerType().equals(this.serverType)) {
            try {
                Integer i = Integer.valueOf(event.getServer().getName().split("-")[1]);
                inventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void OnSync(OnSyncEvent event) {
        this.rebuildUI();
    }

    @EventHandler
    public void onServerUpdate(OnServerUpdateEvent event) {
        try {
            Integer i = Integer.valueOf(event.getServer().getName().split("-")[1]);
            inventory.setItem(i, craftItem(event.getServer()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
