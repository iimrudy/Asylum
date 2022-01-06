package eu.asylum.proxy;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
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
import eu.asylum.common.cloud.enums.QueueChannels;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueConnect;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueJoin;
import eu.asylum.common.cloud.queue.QueueRepository;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.utils.Constants;
import eu.asylum.proxy.configuration.TomlConfigurationContainer;
import eu.asylum.proxy.handler.QueueLimboHandler;
import eu.asylum.proxy.listener.ServerListener;
import lombok.Getter;
import lombok.SneakyThrows;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.player.LimboPlayer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(id = "proxy", name = "Proxy", version = "1.0-SNAPSHOT", description = "I did it!", authors = {"iim_rudy"})
@Getter
public class Proxy {

    private static Proxy instance;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final LimboFactory limboFactory;
    private AsylumProvider<Player> asylumProvider;
    private ServerRepository serverRepostiory;
    private QueueRepository queueRepository;
    private Limbo queueServer;
    private final Map<String, QueueLimboHandler> limboPlayers = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, RegisteredServer> queuedJoin = new ConcurrentHashMap<>();

    @Inject
    public Proxy(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        instance = this;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = folder;
        logger.info("Hello there! I made my first plugin with Velocity.");
        this.limboFactory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
    }

    public static Proxy get() {
        return instance;
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
        this.asylumProvider = new ProxyAsylumProvider(new TomlConfigurationContainer(loadConfig(new File(dataDirectory.toFile(), "AsylumCommon.toml"))));
        server.getEventManager().register(this, this.asylumProvider);
        server.getEventManager().register(this, new ServerListener());
        this.serverRepostiory = new ProxyServerRepository(AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
        this.queueRepository = new ProxyQueueRepository(this.serverRepostiory);

        VirtualWorld queueWorld = this.limboFactory.createVirtualWorld(Dimension.THE_END, 0, 0, 0, 90f, 90f);
        this.queueServer = this.limboFactory.createLimbo(queueWorld);
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
