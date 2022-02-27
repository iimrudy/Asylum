package eu.asylum.lobby;

import co.aikar.commands.BukkitCommandManager;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.core.configuration.YamlConfigurationContainer;
import eu.asylum.core.gui.ServerGui;
import eu.asylum.core.helpers.AsylumScoreBoard;
import eu.asylum.lobby.commands.staff.BuildCommand;
import eu.asylum.lobby.commands.staff.LobbyManagerCommand;
import eu.asylum.lobby.configuration.LobbyConfiguration;
import eu.asylum.lobby.game.GamesManager;
import eu.asylum.lobby.gui.ServerSelectorGUI;
import eu.asylum.lobby.listener.PlayerListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import kr.entree.spigradle.annotations.SpigotPlugin;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@SpigotPlugin
public class AsylumLobby extends JavaPlugin {

  private static final String[] animations =
      new String[] {
        "&5&lASYLUM.EU", "&5&lASYLUM.EU %playerlist_online,normal,yes,amount%", "&5&lASYLUM.EU"
      };

  @Getter private static AsylumLobby instance;
  @Getter private final List<Player> buildingPlayers = new ArrayList<>();
  private ServerSelectorGUI serverSelectorGUI;
  private ServerGui lobbySelectorGUI;
  @Getter private Location lobbyLocation;
  @Getter private GamesManager gamesManager;
  private int animationTick = 0;
  private BukkitCommandManager commandManager;
  private YamlConfigurationContainer configuration;
  private List<String> scoreboardList = new ArrayList<>();
  private String scoreboardTitle;
  private final Runnable scoreboardTask =
      () -> {
        Bukkit.getOnlinePlayers()
            .forEach(
                player -> {
                  AsylumScoreBoard board = AsylumScoreBoard.getByPlayer(player);

                  if (board == null) return;

                  if (!this.scoreboardTitle.isEmpty()) {
                    board.setTitle(PlaceholderAPI.setPlaceholders(player, this.scoreboardTitle));
                  } else {
                    board.setTitle(
                        PlaceholderAPI.setPlaceholders(player, animations[animationTick]));
                  }
                  List<String> placeholdered = new ArrayList<>();
                  for (var s : scoreboardList) {
                    placeholdered.add(PlaceholderAPI.setPlaceholders(player, s));
                  }
                  board.setSlotsFromList(placeholdered);
                });

        animationTick++;
        if (animationTick >= animations.length) animationTick = 0;
      };

  @Override
  public void onEnable() {
    AsylumLobby.instance = this;
    this.getServer().getScheduler().runTaskTimerAsynchronously(this, scoreboardTask, 10L, 60L);
    this.commandManager = new BukkitCommandManager(this);
    this.commandManager.registerCommand(new LobbyManagerCommand());
    this.commandManager.registerCommand(new BuildCommand());
    File path = new File(getDataFolder(), "AsylumLobby.yml");

    try {
      if (!path.exists()) {
        getDataFolder().mkdirs();
        path.createNewFile();
      }
      YamlConfiguration yamlConfiguration = new YamlConfiguration();
      yamlConfiguration.load(path);
      this.configuration = new YamlConfigurationContainer(yamlConfiguration, path);
    } catch (Exception e) {
      throw new RuntimeException(e); // re throw exception so the plugin will be disabled
    }

    this.loadData();
    this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    this.lobbySelectorGUI =
        new ServerGui(
            ServerType.LOBBY,
            LegacyComponentSerializer.legacyAmpersand().deserialize("&e&lLOBBY SELECTOR"),
            this);
    this.gamesManager = new GamesManager();
    this.getServer().getOnlinePlayers().forEach(Items::formatInventory);
    this.serverSelectorGUI = new ServerSelectorGUI(this);
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
