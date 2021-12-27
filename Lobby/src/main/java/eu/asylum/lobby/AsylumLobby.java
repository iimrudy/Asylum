package eu.asylum.lobby;

import co.aikar.commands.BukkitCommandManager;
import eu.asylum.core.configuration.YamlConfigurationContainer;
import eu.asylum.core.helpers.AsylumScoreBoard;
import eu.asylum.lobby.commands.staff.LobbyManagerCommand;
import eu.asylum.lobby.configuration.LobbyConfiguration;
import eu.asylum.lobby.listener.PlayerListener;
import kr.entree.spigradle.annotations.SpigotPlugin;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@SpigotPlugin
public class AsylumLobby extends JavaPlugin {

    private static final String[] animations = new String[]{
            "&5&lASYLUM.EU",
            "&5&lASYLUM.EU %playerlist_online,normal,yes,amount%",
            "&5&lASYLUM.EU"
    };

    @Getter
    private static AsylumLobby instance;
    private int animationTick = 0;
    private BukkitCommandManager commandManager;
    private YamlConfigurationContainer configuration;
    private List<String> scoreboardList = new ArrayList<>();
    private String scoreboardTitle;
    @Getter
    private Location lobbyLocation;
    private final Runnable scoreboardTask = () -> {
        Bukkit.getOnlinePlayers().forEach(player -> {
            AsylumScoreBoard board = AsylumScoreBoard.getByPlayer(player);

            if (board == null) return;

            if (!this.scoreboardTitle.isEmpty()) {
                board.setTitle(this.scoreboardTitle);
            } else {
                board.setTitle(PlaceholderAPI.setPlaceholders(player, animations[animationTick]));
            }

            board.setSlotsFromList(this.scoreboardList);
        });

        animationTick++;

        if (animationTick >= animations.length)
            animationTick = 0;
    };

    @Override
    public void onEnable() {
        instance = this;
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, scoreboardTask, 10L, 60L);
        this.commandManager = new BukkitCommandManager(this);
        this.commandManager.registerCommand(new LobbyManagerCommand());
        File path = new File(getDataFolder(), "AsylumLobby.yml");

        try {
            if (!path.exists()) {
                getDataFolder().mkdirs();
                path.createNewFile();
            }
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(path);
            this.configuration = new YamlConfigurationContainer(configuration, path);
        } catch (Exception e) {
            throw new RuntimeException(e); // re throw exception so the plugin will be disabled
        }

        this.loadData();

        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    }

    public void reload() throws Exception {
        this.configuration.reload(false);
        this.loadData();
    }

    private void loadData() {
        List<String> s = LobbyConfiguration.SCOREBOARD.get(List.class);
        this.scoreboardTitle = s.get(0);
        this.scoreboardList = s.subList(1, s.size());
        this.lobbyLocation = LobbyConfiguration.HUB_SPAWN.get(Location.class);
    }


    @Override
    public void onDisable() {
        this.commandManager.unregisterCommands();
        try {
            this.configuration.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLobbyLocation(Location lobbyLocation) {
        LobbyConfiguration.HUB_SPAWN.set(lobbyLocation);
        this.lobbyLocation = lobbyLocation;
    }
}
