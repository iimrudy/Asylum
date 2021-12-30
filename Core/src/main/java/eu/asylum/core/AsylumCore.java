package eu.asylum.core;

import co.aikar.commands.BukkitCommandManager;
import eu.asylum.common.AsylumProvider;
import eu.asylum.common.cloud.redis.CloudChannels;
import eu.asylum.common.cloud.redis.RedisAsylumServerShutdown;
import eu.asylum.common.cloud.redis.RedisAsylumServerUpdate;
import eu.asylum.common.utils.Constants;
import eu.asylum.core.configuration.YamlConfigurationContainer;
import eu.asylum.core.helpers.AsylumScoreBoard;
import eu.asylum.core.listener.PlayerListener;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import kr.entree.spigradle.annotations.SpigotPlugin;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

@Getter
@SpigotPlugin
public class AsylumCore extends JavaPlugin {

    private static final RedisAsylumServerUpdate redisAsylumServerUpdate = new RedisAsylumServerUpdate();
    @Getter
    private static AsylumCore instance;
    private AsylumProvider<Player> asylumProvider;
    private BukkitCommandManager commandManager;
    @Getter
    private YamlConfigurationContainer configuration;
    private volatile long lastUpdate = 0;
    private String serverName;

    @Override
    public void onEnable() {
        instance = this;
        try {
            Properties properties = new Properties();
            properties.load(new FileReader("asylumserver.properties"));
            serverName = properties.getProperty("serverName", "UNKNOWN-1");
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.shutdown();
        }


        File path = new File(getDataFolder(), "AsylumCommon.yml");
        try {
            if (!path.exists()) {
                getDataFolder().mkdirs();
                path.createNewFile();
            }
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(path);
            this.configuration = new YamlConfigurationContainer(configuration, path);
            asylumProvider = new BukkitAsylumProvider(this.configuration);
        } catch (Exception e) {
            throw new RuntimeException(e); // re throw exception so the plugin will be disabled
        }


        Bukkit.getOnlinePlayers().forEach((player) -> {
            AsylumScoreBoard.createScore(player); // create scoreboard for the players online
            setupPrefix(player);
        });

        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        this.getServer().getPluginManager().registerEvents((Listener) asylumProvider, this);
        this.commandManager = new BukkitCommandManager(this);
        //this.commandManager.registerCommand(new LobbyManagerCommand());
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (System.currentTimeMillis() - lastUpdate > 60000) { // send update every minute
                sendUpdate();
            }
        }, 0, 20L); // send update every minute

        this.getServer().getScheduler().runTaskTimer(this, new TpsCalculator(), 0, 1);
        this.getAsylumProvider().getAsylumDB().getPubSubConnectionReceiver().sync().subscribe(CloudChannels.SERVER_SHUTDOWN.getChannel());
        this.getAsylumProvider().getAsylumDB().getPubSubConnectionReceiver().addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                Bukkit.getScheduler().runTask(AsylumCore.this, () -> {
                    if (channel.equalsIgnoreCase(CloudChannels.SERVER_SHUTDOWN.getChannel())) {
                        getLogger().warning("Received shutdown message from cloud");
                        var msg = Constants.get().getGson().fromJson(message, RedisAsylumServerShutdown.class);
                        if (msg.getServerName().equals(getServerName())) { // is this server?
                            Bukkit.shutdown();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDisable() {
        AsylumScoreBoard.flush(); // flush scoreboards
        asylumProvider.shutdown();
    }

    public void sendUpdate() {
        if (System.currentTimeMillis() - lastUpdate < 1000) { // 1 second of delay between updates
            return;
        }
        lastUpdate = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String serverName = getServerName();
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            double tps = Bukkit.getTPS()[0];
            long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;

            redisAsylumServerUpdate.setServerName(serverName);
            redisAsylumServerUpdate.setOnlinePlayers(onlinePlayers);
            redisAsylumServerUpdate.setTps(tps);
            redisAsylumServerUpdate.setRamUsage(usedMemory);
            redisAsylumServerUpdate.setMotd(getMotd());
            asylumProvider.getAsylumDB().publishJson(CloudChannels.SERVER_UPDATE.getChannel(), redisAsylumServerUpdate);
        });
    }

    public final void setupPrefix(Player player) {
        asylumProvider.getAsylumPlayerAsync(player).thenAccept(asylumPlayer -> {
            if (asylumPlayer.isPresent() && player.isOnline()) {
                var prefix = asylumPlayer.get().getRank().getPrefix();
                if (prefix.length() > 0) {
                    prefix = prefix + " ";
                }
                asylumPlayer.get().getPlayerObject().playerListName(MiniMessage.get().parse(prefix + "<white>" + asylumPlayer.get().getUsername()));
            }
        });
    }

    public String getMotd() {
        return MinecraftServer.getServer().getMotd();
    }

    public void setMotd(String m) {
        MinecraftServer.getServer().setMotd(m);
    }

}
