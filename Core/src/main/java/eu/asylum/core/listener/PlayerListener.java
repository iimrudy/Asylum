package eu.asylum.core.listener;

import eu.asylum.core.AsylumCore;
import eu.asylum.core.helpers.AsylumScoreBoard;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        AsylumScoreBoard.createScore(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(AsylumCore.getInstance(), () -> AsylumScoreBoard.removeScore(event.getPlayer()));
    }

}
