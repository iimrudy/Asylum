package eu.asylum.lobby.game;

import eu.asylum.lobby.AsylumLobby;
import eu.asylum.lobby.Items;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class LobbyGame implements Listener {

  protected List<Player> playerList = new ArrayList<>();

  public LobbyGame() {}

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
