package eu.asylum.core;

import co.aikar.commands.BukkitCommandManager;
import eu.asylum.common.AsylumProvider;
import eu.asylum.common.cloud.enums.CloudChannels;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudShutdown;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudUpdate;
import eu.asylum.common.utils.Constants;
import eu.asylum.core.configuration.YamlConfigurationContainer;
import eu.asylum.core.helpers.AsylumScoreBoard;
import eu.asylum.core.listener.PlayerListener;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import kr.entree.spigradle.annotations.SpigotPlugin;
import lombok.Cleanup;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@SpigotPlugin
public class AsylumCore extends JavaPlugin {

  private static final RedisCloudUpdate REDIS_CLOUD_SERVER_UPDATE = new RedisCloudUpdate();
  @Getter private static AsylumCore instance;
  private AsylumProvider<Player> asylumProvider;
  private BukkitCommandManager commandManager;
  @Getter private YamlConfigurationContainer configuration;
  private volatile long lastUpdate = 0;
  private String serverName;

  @Override
  public void onEnable() {
    AsylumCore.instance = this;
    Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    try {
      Properties properties = new Properties();
      @Cleanup var fr = new FileReader("asylumserver.properties");
      properties.load(fr);
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
      YamlConfiguration yamlConfig = new YamlConfiguration();
      yamlConfig.load(path);
      this.configuration = new YamlConfigurationContainer(yamlConfig, path);
      asylumProvider = new BukkitAsylumProvider(this.configuration);
    } catch (Exception e) {
      throw new RuntimeException(e); // re throw exception so the plugin will be disabled
    }

    Bukkit.getOnlinePlayers()
        .forEach(
            (player) -> {
              AsylumScoreBoard.createScore(player); // create scoreboard for the players online
              setupPrefix(player);
            });

    this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    this.getServer().getPluginManager().registerEvents((Listener) asylumProvider, this);
    this.commandManager = new BukkitCommandManager(this);

    this.getServer()
        .getScheduler()
        .runTaskTimerAsynchronously(
            this,
            () -> {
              if (System.currentTimeMillis() - lastUpdate > 60000) { // send update every minute
                sendUpdate(false);
              }
            },
            0,
            20L); // send update every minute

    this.getAsylumProvider()
        .getAsylumDB()
        .getPubSubConnectionReceiver()
        .sync()
        .subscribe(CloudChannels.SERVER_SHUTDOWN.getChannel());
    this.getAsylumProvider()
        .getAsylumDB()
        .getPubSubConnectionReceiver()
        .addListener(
            new RedisPubSubAdapter<>() {
              @Override
              public void message(String channel, String message) {
                Bukkit.getScheduler()
                    .runTask(
                        AsylumCore.this,
                        () -> {
                          if (channel.equalsIgnoreCase(
                              CloudChannels.SERVER_SHUTDOWN.getChannel())) {
                            getLogger().warning("Received shutdown message from cloud");
                            var msg =
                                Constants.get()
                                    .getGson()
                                    .fromJson(message, RedisCloudShutdown.class);
                            if (msg.getServerName().equals(getServerName())) { // is this server?
                              Bukkit.shutdown();
                            }
                          } else if (channel.equalsIgnoreCase(CloudChannels.SYNC.getChannel())) {
                            Bukkit.getScheduler()
                                .runTaskLater(
                                    AsylumCore.this,
                                    () -> {
                                      sendUpdate(true);
                                    },
                                    20L * 5L);
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

  public void sendUpdate(boolean forced) {
    if (System.currentTimeMillis() - lastUpdate < 50
        && !forced) { // 100 ms of delay between updates
      return;
    }
    lastUpdate = System.currentTimeMillis();
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            this,
            () -> {
              String serverName0 = getServerName();

              int onlinePlayers = Bukkit.getOnlinePlayers().size();
              double tps = Bukkit.getTPS()[0];
              long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
              long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
              long usedMemory = totalMemory - freeMemory;

              REDIS_CLOUD_SERVER_UPDATE.setServerName(serverName0);
              REDIS_CLOUD_SERVER_UPDATE.setOnlinePlayers(onlinePlayers);
              REDIS_CLOUD_SERVER_UPDATE.setTps(tps);
              REDIS_CLOUD_SERVER_UPDATE.setRamUsage(usedMemory);
              REDIS_CLOUD_SERVER_UPDATE.setMotd(getMotd());

              // TODO: depcrecate pubusb update messages
              asylumProvider
                  .getAsylumDB()
                  .publishJson(CloudChannels.SERVER_UPDATE.getChannel(), REDIS_CLOUD_SERVER_UPDATE);
              // TODO: manual updates
              asylumProvider
                  .getRepository()
                  .getAsylumDB()
                  .getRedisConnection()
                  .async()
                  .set(serverName0, Constants.get().getGson().toJson(REDIS_CLOUD_SERVER_UPDATE))
                  .thenAccept(
                      aVoid -> {
                        if (aVoid != null) { // expiration is configured once the key has been
                          // successfully inserted
                          asylumProvider
                              .getRepository()
                              .getAsylumDB()
                              .getRedisConnection()
                              .async()
                              .expire(serverName0, 7L); // 7 seconds in case of delayed update
                        }
                      });
            });
  }

  public final void setupPrefix(Player player) {
    asylumProvider
        .getAsylumPlayerAsync(player)
        .thenAccept(
            asylumPlayer -> {
              if (asylumPlayer.isPresent() && player.isOnline()) {
                var prefix = asylumPlayer.get().getPlayerData().getRank().getPrefix();
                if (prefix.length() > 0) {
                  prefix = prefix + " ";
                }
                asylumPlayer
                    .get()
                    .getPlayerObject()
                    .playerListName(
                        MiniMessage.get()
                            .parse(prefix + "<white>" + asylumPlayer.get().getUsername()));
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
