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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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

    private final List<Server> badServersList = Collections.synchronizedList(new ArrayList<>());
    private final List<String> occupiedNames = new CopyOnWriteArrayList<>();
    private final CommandHandler commandHandler;
    private final TaskWaiter logicWaiter = new TaskWaiter(true);
    private final QueueManager queueManager;

    public Cloud() throws Exception {
        // configuration handling
        File file = new File("./configuration.properties");
        if (!file.exists()) {
            file.createNewFile();
        }
        Properties prop = new Properties();
        try {
            FileReader fr = new FileReader(file);
            prop.load(fr);
            fr.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.configurationContainer = new PropertiesConfiguration(prop, file);
        AsylumConfiguration.setConfigurationContainer(this.configurationContainer);

        this.asylumDB = new AsylumDB(AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
        this.repository = new ServerRepository(AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
        this.commandHandler = new CommandHandler(this.asylumDB, "");

        for (Server srv : this.repository.getServers()) {
            this.occupiedNames.add(srv.getName());
        }
        this.queueManager = new QueueManager(this.repository);
        Constants.get().getExecutor().scheduleAtFixedRate(this::logic0, 5, 30L, TimeUnit.SECONDS);
        Constants.get().getExecutor().scheduleAtFixedRate(this::logic, 5, 120, java.util.concurrent.TimeUnit.SECONDS); // every 2 minutes
    }

    private static int findFreePort() {
        int port = 0;
        // For ServerSocket port number 0 means that the port number is automatically allocated.
        try (ServerSocket socket = new ServerSocket(0)) {
            // Disable timeout and reuse address after closing the socket.
            socket.setReuseAddress(true);
            port = socket.getLocalPort();
        } catch (IOException ignored) {
            // this exception is ignored.
        }
        if (port > 0) {
            return port;
        }
        throw new RuntimeException("Could not find a free port");
    }

    // Thread-Safe singleton getter.
    public static synchronized Cloud getInstance() {
        if (singleton == null) {
            try {
                singleton = new Cloud();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return singleton;
    }

    private void logic0() {
        logicWaiter.await();
        logicWaiter.start();
        HashMap<ServerType, Integer> newServers = Arrays.stream(ServerType.values()).collect(Collectors.toMap(serverType -> serverType, serverType -> 0, (a, b) -> b, HashMap::new));

        // host servers if needed
        for (var type : ServerType.values()) {
            if (type == ServerType.LOBBY) { // only lobbies for now
                int maxPlayers = type.getMaxPlayers();
                int onlinePlayers = this.repository.getOnlinePlayers(type); // 200
                int onlineServers = this.repository.getServers(type).size(); // 60
                int necessary = (onlinePlayers / maxPlayers) + 2; // 200 / 60 = 3
                if (necessary > onlineServers) {
                    int toHost = (necessary - onlineServers);
                    logger.log("Hosting " + toHost + " " + type.name() + " servers.");
                    newServers.put(type, newServers.get(type) + toHost);
                }
            }
        }
        multiHoster(newServers, "NEEDED");
        logicWaiter.finish();
    }

    private void logic() {
        logicWaiter.await();
        logicWaiter.start();
        logger.log("Starting logic");
        // host servers if needed
        // stop laggy servers - (don't stop in game servers)
        HashMap<ServerType, Integer> newServers = Arrays.stream(ServerType.values()).collect(Collectors.toMap(serverType -> serverType, serverType -> 0, (a, b) -> b, HashMap::new));
        var toKill = new ArrayList<Server>();

        for (var server : this.repository.getServers()) {

            var isBadServer0 = isLaggyOrUnReachable(server);
            var isBadServer = isBadServer0.getT1() || isBadServer0.getT2();
            var contains = this.badServersList.contains(server);
            if (isBadServer) {
                if (contains) {
                    if (server.getServerType().canClose(server) && !isBadServer0.getT1()) { // the server is reachable but laggy
                        logger.warning("Closing laggy server " + server.getName());
                        toKill.add(server);
                        newServers.replace(server.getServerType(), newServers.get(server.getServerType()) + 1);
                    }
                } else {
                    this.badServersList.add(server);
                }
            } else if (contains) {
                this.badServersList.remove(server);
            }
        }

        multiHoster(newServers, "lag/not reachable");

        // severs are killed here - no interference between starting and killing a server.
        for (var server : toKill) {
            graciouslyKill(server);
        }
        logicWaiter.finish();
    }

    private Tuple2<Boolean, Boolean> isLaggyOrUnReachable(@NonNull Server server) {
        return Tuples.of(!server.getPinger().ping(), server.getServerStatus().getTps() < LAGGY_TPS);
    }

    private void multiHoster(Map<ServerType, Integer> newServers, String cause) {
        for (var entry : newServers.entrySet()) {
            IntStream.range(0, entry.getValue()).forEach(i -> hostServer(entry.getKey()).ifPresent(server -> logger.log("Hosting a new server (" + cause + ") " + server.getName())));
        }
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
        Constants.get().getExecutor().schedule(() -> this.occupiedNames.remove(server.getName()), 30L, TimeUnit.SECONDS); //
    }

    private void announceServerKill(@NonNull Server server) {
        logger.log("Server " + server.getName() + " is being announce killed");
        asylumDB.getMongoCollection("asylum", "cloud").findOneAndDelete(new Document().append("name", server.getName()));
        asylumDB.publishJson(CloudChannels.SERVER_DELETE.getChannel(), new RedisCloudDelete(server));
        // cleanup from the checks lists
        this.badServersList.remove(server);
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
