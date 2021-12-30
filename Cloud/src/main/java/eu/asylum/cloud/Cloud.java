package eu.asylum.cloud;

import eu.asylum.cloud.shell.CommandHandler;
import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.redis.*;
import eu.asylum.common.cloud.servers.Server;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.configuration.PropertiesConfiguration;
import eu.asylum.common.database.AsylumDB;
import eu.asylum.common.mongoserializer.MongoSerializer;
import eu.asylum.common.utils.Constants;
import eu.asylum.common.utils.SyncConsoleCommand;
import eu.asylum.common.utils.ZipUtils;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.hydev.logger.HyLogger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Cloud {
    private static Cloud singleton;


    private final ConfigurationContainer<?> configurationContainer;
    private final AsylumDB asylumDB;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final HyLogger logger = new HyLogger("Cloud");
    @Getter
    private final ServerRepository repository;

    public Cloud() throws Exception {
        Cloud.singleton = this;

        // configuration handling
        File file = new File("./configuration.properties");
        if (!file.exists()) {
            file.createNewFile();
        }
        Properties prop = new Properties();
        prop.load(new FileReader(file));
        this.configurationContainer = new PropertiesConfiguration(prop, file);
        AsylumConfiguration.setConfigurationContainer(this.configurationContainer);

        this.asylumDB = new AsylumDB(AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
        this.repository = new ServerRepository(this.asylumDB);

        this.asylumDB.getPubSubConnectionReceiver().addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                if (channel.equals(CloudChannels.SERVER_UPDATE.getChannel())) {
                    // a server, send his own information, ram usage, online players, tps, etc
                    var update = Constants.get().getGson().fromJson(message, RedisAsylumServerUpdate.class);
                    //logger.log("Received update from " + update.toString());
                } else if (channel.equals(CloudChannels.SERVER_DELETE.getChannel())) {
                    var delete = Constants.get().getGson().fromJson(message, RedisAsylumServerDelete.class);
                } else if (channel.equals(CloudChannels.SERVER_ADD.getChannel())) {
                    var add = Constants.get().getGson().fromJson(message, RedisAsylumServerAdd.class);
                }
            }
        });

        Constants.get().getExecutor().scheduleAtFixedRate(this::logic, 5, 120, java.util.concurrent.TimeUnit.SECONDS); // every 2 minutes
        new CommandHandler().run(); // start the command handler once everything is loaded
    }

    private static int findFreePort() {
        int port = 0;
        // For ServerSocket port number 0 means that the port number is automatically allocated.
        try (ServerSocket socket = new ServerSocket(0)) {
            // Disable timeout and reuse address after closing the socket.
            socket.setReuseAddress(true);
            port = socket.getLocalPort();
        } catch (IOException ignored) {
        }
        if (port > 0) {
            return port;
        }
        throw new RuntimeException("Could not find a free port");
    }

    // Thread-Safe singleton getter.
    public static synchronized Cloud getInstance() {
        if (singleton == null) {
            synchronized (Cloud.class) {
                if (singleton == null) {
                    try {
                        new Cloud();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return singleton;
    }

    private final void logic() {
        while (running.get()) {
            // host servers if needed
            // stop laggy servers - (don't stop in game servers)
        }
    }

    public Optional<Server> hostServer(ServerType type) {
        int port = findFreePort();
        String name = getFirstFreeServerName(type);
        File pathTo = new File("./servers/" + name);
        File templatePath = new File("./template/" + type.getZipFile());
        if (pathTo.exists()) {
            if (type.isPersistent()) {
                return hostServer0(type.createServer(name, "127.0.0.1", port)); // start the server - cuz is persistent we can reuse old files
            } else {
                try {
                    FileUtils.deleteDirectory(pathTo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            ZipUtils.unzip(pathTo, templatePath);
            return hostServer0(type.createServer(name, "127.0.0.1", port)); // start the server here
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.error("Failed to host server, cant' create folders");
        return Optional.empty();
    }

    public final void graciouslyKill(@NonNull Server server) {
        var confirm = new RedisAsylumServerShutdown();
        confirm.setServerName(server.getName());
        asylumDB.publishJson(CloudChannels.SERVER_SHUTDOWN.getChannel(), confirm);
        Constants.get().getExecutor().schedule(() -> {
            var start = System.currentTimeMillis();
            var t = 0L;
            while (true) {
                if (System.currentTimeMillis() - t > 500) {
                    if (!server.getPinger().ping() && server.getPinger().getPingVersion() == -1) {
                        cleanUpServer(server);
                        break;
                    }
                    t = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - start > 60000) { // 1 minute - server is not responding to shutdown.
                    forceKill(server);
                    break;
                }
            }
        }, 10, TimeUnit.SECONDS);
    }

    public final void forceKill(@NonNull Server server) {
        new SyncConsoleCommand("fuser -k " + server.getPort() + "/tcp",
                stringList -> logger.log("Server Killed result #-> " + stringList),
                exception -> {
                    exception.printStackTrace();
                    logger.error("Can't kill the server");
                });
        cleanUpServer(server);
    }

    // Clean up server directory & send remove info to the database
    private void cleanUpServer(@NonNull Server server) {
        asylumDB.getMongoCollection("asylum", "cloud").findOneAndDelete(new Document().append("name", server.getName()));
        asylumDB.publishJson(CloudChannels.SERVER_DELETE.getChannel(), new RedisAsylumServerDelete(server));
        if (!server.getServerType().isPersistent()) {
            try {
                FileUtils.deleteDirectory(new File("./servers/" + server.getName()));
            } catch (IOException e) {
                logger.error("Can't delete the server folder... " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private Optional<Server> hostServer0(Server server) {
        AtomicBoolean success = new AtomicBoolean(false);
        new SyncConsoleCommand("bash startserver.sh " + server.getName() + " " + server.getPort() + " " + server.getMinRam() + " " + server.getMaxRam(), logs -> {
            logger.log("Server Executed.");
            asylumDB.publishJson(CloudChannels.SERVER_ADD.getChannel(), new RedisAsylumServerAdd(server));
            asylumDB.getMongoCollection("asylum", "cloud").insertOne(MongoSerializer.serialize(server));
            success.set(true);
        }, exc -> logger.error("Error while starting the server --> " + exc.getMessage()));
        if (success.get()) return Optional.of(server);
        return Optional.empty();
    }

    public final String getFirstFreeServerName(ServerType type) {
        int i = 0;
        String name = type.name() + "-" + i;
        while (repository.getByName(name).isPresent()) {
            name = type.name() + "-" + (++i);
        }
        return name;
    }

    public void requestSync() {
        asylumDB.publishMessage(CloudChannels.SYNC.getChannel(), "");
    }

    @Synchronized
    public void stopCloud() {
        this.running.set(false);
        Constants.get().getExecutor().shutdownNow();
    }

    public boolean isRunning() {
        return running.get();
    }
}
