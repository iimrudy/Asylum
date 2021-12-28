package eu.asylum.lobby.games;

import eu.asylum.lobby.AsylumLobby;
import eu.asylum.lobby.Items;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class LobbyGame implements Listener {

    protected List<Player> playerList = new ArrayList<>();

    public LobbyGame() {

    }

    public boolean isPlayerPlaying(Player player) {
        return playerList.contains(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        quit(event.getPlayer());
    }

    public abstract void join(Player player);

    public void quit(Player player) {
        if (playerList.remove(player)) {
            Items.formatInventory(player);
            player.teleport(AsylumLobby.getInstance().getLobbyLocation());
        }
    }

}
