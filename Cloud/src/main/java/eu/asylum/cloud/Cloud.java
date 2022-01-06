package eu.asylum.cloud;

import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.command.CommandHandler;
import eu.asylum.common.cloud.enums.CloudChannels;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudAdd;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudDelete;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudShutdown;
import eu.asylum.common.cloud.servers.Server;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.configuration.PropertiesConfiguration;
import eu.asylum.common.database.AsylumDB;
import eu.asylum.common.mongoserializer.MongoSerializer;
import eu.asylum.common.utils.Constants;
import eu.asylum.common.utils.SyncConsoleCommand;
import eu.asylum.common.utils.TaskWaiter;
import eu.asylum.common.utils.ZipUtils;
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
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static eu.asylum.common.cloud.ServerRepository.LAGGY_TPS;

public class Cloud {
    private static Cloud singleton;

    private final ConfigurationContainer<?> configurationContainer;
    private final AsylumDB asylumDB;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final HyLogger logger = new HyLogger("Cloud");
    @Getter
    private final ServerRepository repository;

    private final List<Server> notReachableServers = Collections.synchronizedList(new ArrayList<>());
    private final List<Server> laggyServers = Collections.synchronizedList(new ArrayList<>());
    private final List<String> occupiedNames = new CopyOnWriteArrayList<>();
    private final CommandHandler commandHandler;
    private final TaskWaiter logicWaiter = new TaskWaiter(true);
    private final QueueManager queueManager;

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
        this.repository = new ServerRepository(AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
        this.commandHandler = new CommandHandler(this.asylumDB, "");

        /*this.asylumDB.getPubSubConnectionReceiver().addListener(new RedisPubSubAdapter<>() {
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
        });*/
        this.queueManager = new QueueManager(this.repository);
        Constants.get().getExecutor().scheduleAtFixedRate(this::logic0, 5, 30L, TimeUnit.SECONDS);
        Constants.get().getExecutor().scheduleAtFixedRate(this::logic, 5, 120, java.util.concurrent.TimeUnit.SECONDS); // every 2 minutes
        new eu.asylum.cloud.shell.CommandHandler().run(); // start the command handler once everything is loaded
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
    public static Cloud getInstance() {
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

    private void logic0() {
        logicWaiter.await();
        logicWaiter.start();
        var newServers = new HashMap<ServerType, Integer>();
        for (var serverType : ServerType.values()) {
            newServers.put(serverType, 0);
        }

        // host servers if needed
        for (var type : ServerType.values()) {
            if (type == ServerType.LOBBY) { // only lobbies for now
                int maxPlayers = type.getMaxPlayers();
                int onlinePlayers = this.repository.getOnlinePlayers(type); // 200
                int onlineServers = this.repository.getServers(type).size(); // 60
                int necessary = (onlinePlayers / maxPlayers); // 200 / 60 = 3
                if (onlineServers <= necessary) {
                    int toHost = (onlineServers - necessary) + 2;
                    newServers.put(type, newServers.get(type) + toHost);
                }
            }
        }
        for (var entry : newServers.entrySet()) {
            IntStream.range(0, entry.getValue()).forEach(i -> {
                hostServer(entry.getKey()).ifPresent(server -> logger.log("Hosting a new server (cause NEEDED) " + server.getName()));
            });
        }
        logicWaiter.finish();
    }

    private void logic() {
        logicWaiter.await();
        logicWaiter.start();
        logger.log("Starting logic");
        // host servers if needed
        // stop laggy servers - (don't stop in game servers)
        var newServers = new HashMap<ServerType, Integer>();
        var toKill = new ArrayList<Server>();
        for (var serverType : ServerType.values()) {
            newServers.put(serverType, 0);
        }

        for (var server : this.repository.getServers()) {
            if (server.getPinger().ping()) { // is server pingable ?
                this.notReachableServers.remove(server); // if was not reachable, remove it the server is now reachable
                if (server.getServerStatus() != null) {
                    if (server.getServerStatus().getTps() < LAGGY_TPS) {
                        // this is a laggy server
                        if (this.laggyServers.contains(server)) {
                            if (server.getServerType().canClose(server)) { // can the server be closed ?
                                logger.warning("Closing laggy server " + server.getName());
                                toKill.add(server);
                                // graciouslyKill(server);
                                newServers.replace(server.getServerType(), newServers.get(server.getServerType()) + 1);
                            }
                        } else {
                            this.laggyServers.add(server);
                        }
                    } else {
                        this.laggyServers.remove(server); // remove cuz the server is not laggy anymore
                    }
                }
            } else { // maybe is a dead server
                if (this.notReachableServers.contains(server)) { // not reachable, maybe dead - going to kill it
                    logger.warning("Server " + server.getName() + " is not reachable anymore, Killing it.");
                    toKill.add(server);
                    // graciouslyKill(server); // this is a dead server
                    newServers.replace(server.getServerType(), newServers.get(server.getServerType()) + 1);
                } else { // it's not dead, but it's not reachable
                    this.notReachableServers.add(server);
                }
            }
        }

        for (var entry : newServers.entrySet()) {
            IntStream.range(0, entry.getValue()).forEach(i -> {
                hostServer(entry.getKey()).ifPresent(server -> logger.log("Hosting a new server (cause lag/unreachable) " + server.getName()));
            });
        }
        // severs are killed here - no interference between starting and killing a server.
        for (var server : toKill) {
            graciouslyKill(server);
        }
        logicWaiter.finish();
    }

    private Optional<Server> hostServer0(Server server) {
        AtomicBoolean success = new AtomicBoolean(false); // atomic boolean to check if the server was successfully hosted
        new SyncConsoleCommand("bash startserver.sh " + server.getName() + " " + server.getPort() + " " + server.getServerType().getMinRam() + " " + server.getServerType().getMaxRam(), logs -> {
            logger.log("Server Executed.");
            asylumDB.publishJson(CloudChannels.SERVER_ADD.getChannel(), new RedisCloudAdd(server)); // announcing that the server is started
            asylumDB.getMongoCollection("asylum", "cloud").insertOne(MongoSerializer.serialize(server)); // saving the server in the database
            success.set(true); // server was successfully hosted
        }, exc -> logger.error("Error while starting the server --> " + exc.getMessage()) /* wtf error while starting the server - problem occurred while executing the command*/
        );
        if (success.get()) return Optional.of(server);
        return Optional.empty();
    }


    public Optional<Server> hostServer(ServerType type) {
        int port = findFreePort(); // get a port
        String name = getFirstFreeServerName(type); // get server name
        Constants.get().getExecutor().schedule(() -> {
            this.occupiedNames.remove(name); // remove the name from the list of occupied names
        }, 60L, TimeUnit.SECONDS);
        File pathTo = new File("./servers/" + name); // init server path
        File templatePath = new File("./template/" + type.getZipFile()); // get template path
        if (pathTo.exists()) { // if the server-folder already exists
            if (type.isPersistent()) { // if the server is persistent - aka data can be saved & reused
                return hostServer0(type.createServer(name, "127.0.0.1", port)); // start the server - cuz is persistent we can reuse old files
            } else { // server is not persistent, cloud had some problem while removing files (?) - cleaning the server folder
                try {
                    FileUtils.deleteDirectory(pathTo);
                } catch (IOException e) {
                    // ignored - it's not that much a problem if folder is not deleted
                }
            }
        }
        try {
            if (ZipUtils.unzip(pathTo, templatePath)) { // unzip the server to the directory, returns true if success
                return hostServer0(type.createServer(name, "127.0.0.1", port)); // start the server here
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.error("Failed to host server, cant' work with folders");
        return Optional.empty();
    }

    /**
     * Graciously kills the server, waits for it to stop and then removes it from the list of servers
     * stop is received when the server is not responding to the ping anymore
     * if the killing-task is still running after 60seconds, the server is killed forcefully by stopping the process
     */
    public final void graciouslyKill(@NonNull Server server) {
        var confirm = new RedisCloudShutdown();
        confirm.setServerName(server.getName());
        asylumDB.publishJson(CloudChannels.SERVER_SHUTDOWN.getChannel(), confirm);
        announceServerKill(server);
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
                if (System.currentTimeMillis() - start > 60000) { // 1 minute - server is not responding to shut down.
                    forceKill(server);
                    break;
                }
            }
        }, 10, TimeUnit.SECONDS);
    }

    public final void forceKill(@NonNull Server server) {
        announceServerKill(server); // announce the server death
        new SyncConsoleCommand("fuser -k " + server.getPort() + "/tcp",
                stringList -> logger.log("Server Killed result #-> " + stringList),
                exception -> {
                    exception.printStackTrace();
                    logger.error("Can't kill the server");
                });
        cleanUpServer(server); // cleanup server dir & remove from the lists
    }

    // Clean up server directory & send remove info to the database
    private void cleanUpServer(@NonNull Server server) {
        if (!server.getServerType().isPersistent()) { // if the server is not persistent - we can remove the server folder
            try {
                FileUtils.deleteDirectory(new File("./servers/" + server.getName()));
            } catch (IOException e) {
                logger.error("Can't delete the server folder... " + e.getMessage());
                e.printStackTrace();
            }
        }
        this.occupiedNames.remove(server.getName()); // remove the server name from the list of occupied names
    }

    private void announceServerKill(@NonNull Server server) {
        logger.log("Server " + server.getName() + " is being announce killed");
        asylumDB.getMongoCollection("asylum", "cloud").findOneAndDelete(new Document().append("name", server.getName()));
        asylumDB.publishJson(CloudChannels.SERVER_DELETE.getChannel(), new RedisCloudDelete(server));
        // cleanup from the checks lists
        this.notReachableServers.remove(server);
        this.laggyServers.remove(server);
    }


    public final String getFirstFreeServerName(ServerType type) {
        int i = 0;
        String name = type.name() + "-" + i;
        while (repository.getByName(name).isPresent() && !this.occupiedNames.contains(name)) {
            name = type.name() + "-" + (++i);
        }
        return name;
    }

    public void requestSync() {
        asylumDB.publishMessage(CloudChannels.SYNC.getChannel(), ""); // send sync requests to the servers, so all the servers will refersh ServerRepository
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
