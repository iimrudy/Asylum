package eu.asylum.proxy;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.asylum.common.AsylumProvider;
import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.queue.QueueRepository;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.proxy.commands.PunishmentCommand;
import eu.asylum.proxy.commands.QueueCommand;
import eu.asylum.proxy.configuration.TomlConfigurationContainer;
import eu.asylum.proxy.handler.QueueLimboHandler;
import eu.asylum.proxy.listener.ServerListener;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.Getter;
import lombok.SneakyThrows;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

@Plugin(
    id = "proxy",
    name = "Proxy",
    version = "1.0-SNAPSHOT",
    description = "I did it!",
    authors = {"iim_rudy"})
@Getter
public class Proxy {

  public static final char HEAVY_VERTICAL = '\u2503';

  public static final Function<String, Component> serialize =
      message -> LegacyComponentSerializer.legacyAmpersand().deserialize(message);
  private static Proxy instance;
  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private final LimboFactory limboFactory;
  private final Map<String, QueueLimboHandler> limboPlayers = new ConcurrentHashMap<>();
  @Getter private final Map<String, RegisteredServer> queuedJoin = new ConcurrentHashMap<>();
  private AsylumProvider<Player> asylumProvider;
  private ServerRepository serverRepository;
  private QueueRepository queueRepository;
  private Limbo queueServer;

  @Inject
  public Proxy(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
    Proxy.instance = this;
    this.server = server;
    this.logger = logger;
    this.dataDirectory = folder;
    logger.info("Hello there! I made my first plugin with Velocity.");
    this.limboFactory =
        (LimboFactory)
            this.server
                .getPluginManager()
                .getPlugin("limboapi")
                .flatMap(PluginContainer::getInstance)
                .orElseThrow();
  }

  public static Proxy get() {
    return instance;
  }

  public static AsylumProvider<Player> getAsylumProvider() {
    return instance.asylumProvider;
  }

  public static QueueRepository getQueueRepository() {
    return instance.queueRepository;
  }

  private void registerCommand(SimpleCommand command, String name, String... aliases) {
    this.getServer().getCommandManager().register(name, command, aliases);
  }

  @SneakyThrows
  private Toml loadConfig(File file) {
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
      file.createNewFile();
    }
    return new Toml().read(file);
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    logger.info("Proxy initialized!");
    this.asylumProvider =
        new ProxyAsylumProvider(
            new TomlConfigurationContainer(
                loadConfig(new File(dataDirectory.toFile(), "AsylumCommon.toml"))));
    server.getEventManager().register(this, this.asylumProvider);
    server.getEventManager().register(this, new ServerListener());
    this.serverRepository =
        new ProxyServerRepository(
            AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
    this.queueRepository = new ProxyQueueRepository(this.serverRepository);

    VirtualWorld queueWorld =
        this.limboFactory.createVirtualWorld(Dimension.THE_END, 0, 0, 0, 90f, 90f);
    this.queueServer = this.limboFactory.createLimbo(queueWorld);
    registerCommand(new PunishmentCommand(), "ban", "mute", "tempban", "tempmute", "kick");
    registerCommand(new QueueCommand(), "queue", "join");
  }

  public void registerQueueLimbo(String username, QueueLimboHandler queueLimboHandler) {
    if (limboPlayers.containsKey(username)) {
      limboPlayers.replace(username, queueLimboHandler);
    } else {
      limboPlayers.put(username, queueLimboHandler);
    }
  }

  public Optional<QueueLimboHandler> getQueueLimboHandler(String username) {
    return Optional.ofNullable(limboPlayers.get(username));
  }

  public void unregisterQueueLimbo(String username) {
    limboPlayers.remove(username);
  }
}
