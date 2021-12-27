package eu.asylum.lobby.listener;

import eu.asylum.lobby.AsylumLobby;
import eu.asylum.lobby.Items;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public final void onJoin(PlayerJoinEvent event) {
        event.getPlayer().teleport(AsylumLobby.getInstance().getLobbyLocation());
        Items.formatInventory(event.getPlayer());
    }

}
