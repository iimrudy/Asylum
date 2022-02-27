package eu.asylum.core;

import eu.asylum.common.AsylumProvider;
import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.servers.Server;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.player.AbstractAsylumPlayer;
import eu.asylum.core.events.OnServerAddEvent;
import eu.asylum.core.events.OnServerDeleteEvent;
import eu.asylum.core.events.OnServerUpdateEvent;
import eu.asylum.core.events.OnSyncEvent;
import eu.asylum.core.player.BukkitAsylumPlayer;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitAsylumProvider extends AsylumProvider<Player> implements Listener {

  public BukkitAsylumProvider(ConfigurationContainer<YamlConfiguration> configurationContainer) {
    super(configurationContainer);
  }

  @Override
  public List<Player> getOnlinePlayers() {
    return new ArrayList<>(Bukkit.getOnlinePlayers());
  }

  @Override
  public AbstractAsylumPlayer<Player> craftAsylumPlayer(@NonNull Player playerObject) {
    return new BukkitAsylumPlayer(playerObject);
  }

  @EventHandler
  public void onPlayerJoinEvent(@NonNull PlayerJoinEvent event) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(AsylumCore.getInstance(), () -> onJoin(event.getPlayer()));
    AsylumCore.getInstance().setupPrefix(event.getPlayer());
  }

  @EventHandler
  public void onPlayerQuitEvent(@NonNull PlayerQuitEvent event) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(AsylumCore.getInstance(), () -> onQuit(event.getPlayer()));
  }

  @Override
  public @NonNull ServerRepository serverRepositoryBuilder() {
    return new BukkitServerRepository(
        AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
  }

  private final class BukkitServerRepository extends ServerRepository {

    public BukkitServerRepository(String redisUri, String mongoUri) {
      super(redisUri, mongoUri);
    }

    private void callEvent(Event e) {
      Bukkit.getScheduler()
          .runTask(AsylumCore.getInstance(), () -> Bukkit.getPluginManager().callEvent(e));
    }

    @Override
    public void onServerAdd(@NonNull Server server) {
      callEvent(new OnServerAddEvent(server));
    }

    @Override
    public void onServerDelete(@NonNull Server server) {
      callEvent(new OnServerDeleteEvent(server));
    }

    @Override
    public void onServerUpdate(@NonNull Server server) {
      callEvent(new OnServerUpdateEvent(server));
    }

    @Override
    public void onSync() {
      callEvent(new OnSyncEvent());
    }
  }
}
