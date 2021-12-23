package eu.asylum.core;

import co.aikar.commands.PaperCommandManager;
import eu.asylum.common.AsylumProvider;
import eu.asylum.core.configuration.YamlConfigurationContainer;
import eu.asylum.core.helpers.AsylumScoreBoard;
import eu.asylum.core.listener.PlayerListener;
import kr.entree.spigradle.annotations.SpigotPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@Getter
@SpigotPlugin
public class AsylumCore extends JavaPlugin {

    @Getter
    private static AsylumCore instance;
    private AsylumProvider<Player> asylumProvider;
    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        File path = new File(getDataFolder(), "AsylumCommon.yml");

        try {
            if (!path.exists()) {
                getDataFolder().mkdirs();
                path.createNewFile();
            }
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(path);
            YamlConfigurationContainer container = new YamlConfigurationContainer(configuration, path);
            asylumProvider = new BukkitAsylumProvider(container);
        } catch (Exception e) {
            throw new RuntimeException(e); // re throw exception so the plugin will be disabled
        }

        Bukkit.getOnlinePlayers().forEach(AsylumScoreBoard::createScore); // create score for the players online

        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        this.getServer().getPluginManager().registerEvents((Listener) asylumProvider, this);
        this.commandManager = new PaperCommandManager(this);
        //this.commandManager.registerCommand(new LobbyManagerCommand());
    }

    @Override
    public void onDisable() {
        AsylumScoreBoard.flush(); // flush scoreboards
        asylumProvider.shutdown();
    }

}
