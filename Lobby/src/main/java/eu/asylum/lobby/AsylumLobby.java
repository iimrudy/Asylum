package eu.asylum.lobby;

import co.aikar.commands.PaperCommandManager;
import eu.asylum.core.configuration.YamlConfigurationContainer;
import eu.asylum.core.helpers.AsylumScoreBoard;
import eu.asylum.lobby.commands.staff.LobbyManagerCommand;
import kr.entree.spigradle.annotations.SpigotPlugin;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

    private final Runnable scoreboardTask = () -> {
        Bukkit.getOnlinePlayers().forEach(player -> {
            AsylumScoreBoard board = AsylumScoreBoard.getByPlayer(player);

            if (board == null) return;

            board.setTitle(PlaceholderAPI.setPlaceholders(player, animations[animationTick]));
            board.setSlotsFromList(Arrays.asList("", "line2", "line3", "", "Online player: &e&l" + Bukkit.getOnlinePlayers().size()));
        });

        animationTick++;

        if (animationTick >= animations.length)
            animationTick = 0;
    };
    private PaperCommandManager commandManager;
    private YamlConfigurationContainer configuration;

    @Override
    public void onEnable() {
        instance = this;
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, scoreboardTask, 10L, 60L);
        this.commandManager = new PaperCommandManager(this);
        this.commandManager.registerCommand(new LobbyManagerCommand());
        File path = new File(getDataFolder(), "AsylumCommon.yml");

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

}
