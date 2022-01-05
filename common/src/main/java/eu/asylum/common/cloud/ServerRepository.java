package eu.asylum.common.cloud;

import eu.asylum.common.cloud.enums.CloudChannels;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudAdd;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudDelete;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudUpdate;
import eu.asylum.common.cloud.servers.Server;
import eu.asylum.common.database.AsylumDB;
import eu.asylum.common.mongoserializer.MongoSerializer;
import eu.asylum.common.utils.Constants;
import eu.asylum.common.utils.TaskWaiter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerRepository extends RedisPubSubAdapter<String, String> {

    public static final double LAGGY_TPS = 16.0d;
    private final Map<ServerType, List<Server>> servers = new ConcurrentHashMap<>();
    @Getter
    private final AsylumDB asylumDB;
    private volatile TaskWaiter syncWaiter = new TaskWaiter(false);


    public ServerRepository(String redisUri, String mongoUri) {
        RedisURI uri = RedisURI.create(redisUri);
        //uri.setDatabase(Constants.get().REDIS_DB_CLOUD);
        this.asylumDB = new AsylumDB(uri, mongoUri);

        for (CloudChannels channels : CloudChannels.values()) {
            asylumDB.getPubSubConnectionReceiver().sync().subscribe(channels.getChannel());
        }
        this.sync();
        this.asylumDB.getPubSubConnectionReceiver().addListener(this);
    }

    private void sync() {
        syncWaiter.start();
        this.servers.clear();

        for (var st : ServerType.values()) {
            servers.put(st, Collections.synchronizedList(new ArrayList<>()));
        }
        // fetch servers from db
        var collection = asylumDB.getMongoCollection("asylum", "cloud");
        collection.find().forEach((document) -> {
            Server server = MongoSerializer.deserialize(document, Server.class);
            this.servers.get(server.getServerType()).add(server);
        });
        for (var st : ServerType.values()) {
            System.out.println("Loaded " + this.servers.get(st).size() + " " + st.name() + " servers from database.");
        }
        syncWaiter.finish();
    }

    @Override
    @Synchronized
    public void message(String channel, String message) {
        syncWaiter.await(); // wait the sync to finish
        if (channel.equals(CloudChannels.SERVER_UPDATE.getChannel())) {
            // a server, send his own information, ram usage, online players, tps, etc
            var update = Constants.get().getGson().fromJson(message, RedisCloudUpdate.class);
            var optionalServer = getByName(update.getServerName());
            if (optionalServer.isPresent()) {
                optionalServer.get().setServerStatus(update);
                this.onServerUpdate(optionalServer.get()); // call onServerUpdate event
            } else {
                System.out.println("ServerRepository: Received update for unknown server " + update.getServerName());
            }
        } else if (channel.equals(CloudChannels.SERVER_DELETE.getChannel())) {
            var delete = Constants.get().getGson().fromJson(message, RedisCloudDelete.class);
            var c = getByName(delete.getServer().getName());
            if (removeServer(delete.getServer().getName())) {
                c.ifPresent(this::onServerDelete); // call onServerDelete event
                // System.out.println("--== Unregistered a new Server (" + delete.getServer().getName() + ") ==--");
            } else {
                System.out.println("ServerRepository: Server " + delete.getServer().getName() + " not deleted idk whys");
            }
        } else if (channel.equals(CloudChannels.SERVER_ADD.getChannel())) {
            var add = Constants.get().getGson().fromJson(message, RedisCloudAdd.class);
            servers.get(add.getServer().getServerType()).add(add.getServer());
            this.onServerAdd(add.getServer()); // call onServerAdd event
            // System.out.println("--== Registered a new Server (" + add.getServer().getName() + ") ==--");
        } else if (channel.equals(CloudChannels.SYNC.getChannel())) {
            System.out.println("--== Sync Request Received ==--");
            sync();
            this.onSync(); // call onSync event
        }
    }

    public final Optional<Server> getByName(@NonNull String serverName) {
        for (var s : servers.entrySet()) {
            for (var server : s.getValue()) {
                if (server.getName().equals(serverName)) {
                    return Optional.of(server);
                }
            }
        }
        return Optional.empty();
    }

    private boolean removeServer(@NonNull String serverName) {
        var s = getByName(serverName);
        if (s.isPresent()) {
            return removeServer(s.get());
        }
        return false;
    }

    private boolean removeServer(@NonNull Server server) {
        for (var s : servers.entrySet()) {
            for (var srv : s.getValue()) {
                if (srv.equals(server)) {
                    return s.getValue().remove(server);
                }
            }
        }
        return false;
    }

    public List<Server> getLaggyServer(@NonNull ServerType type) {
        return this.servers.get(type).stream().filter(s -> s.getServerStatus().getTps() < LAGGY_TPS).toList();
    }

    public List<Server> getServers(@NonNull ServerType serverType) {
        return Collections.unmodifiableList(servers.get(serverType));
    }

    public List<Server> getServers() {
        List<Server> s = new ArrayList<>();
        this.servers.values().forEach(s::addAll);
        return s;
    }

    public int getServerCount() {
        return this.servers.values().stream().mapToInt(List::size).sum();
    }

    public int getServerCount(@NonNull ServerType type) {
        return this.servers.get(type).size();
    }

    public int getOnlinePlayers() {
        int count = 0;
        for (Server srv : getServers()) {
            if (srv.getServerStatus() != null) {
                count += srv.getServerStatus().getOnlinePlayers();
            }
        }
        return count;
    }

    public int getOnlinePlayers(@NonNull ServerType type) {
        int count = 0;
        for (Server srv : getServers(type)) {
            if (srv.getServerStatus() != null) {
                count += srv.getServerStatus().getOnlinePlayers();
            }
        }
        return count;
    }

    public void onServerUpdate(@NonNull Server server) {
    }

    public void onServerDelete(@NonNull Server server) {
    }

    public void onServerAdd(@NonNull Server server) {
    }

    public void onSync() {
    }


}
