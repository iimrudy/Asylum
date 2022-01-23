package eu.asylum.lobby.game;

import eu.asylum.lobby.AsylumLobby;
import eu.asylum.lobby.game.s.FFALobbyGame;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GamesManager {

    private final List<LobbyGame> gameList = new ArrayList<>();

    public GamesManager() {
        this.register(new FFALobbyGame());
    }

    private void register(LobbyGame g) {
        this.gameList.add(g);
        AsylumLobby.getInstance().getServer().getPluginManager().registerEvents(g, AsylumLobby.getInstance());
        AsylumLobby.getInstance().getLogger().info("Registered game: " + g.getClass().getName());
    }

    public boolean isPlayerPlaying(Player p) {
        for (LobbyGame g : this.gameList) {
            if (g.isPlayerPlaying(p)) {
                return true;
            }
        }
        return false;
    }

}
