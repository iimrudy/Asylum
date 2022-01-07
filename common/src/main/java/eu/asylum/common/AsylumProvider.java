package eu.asylum.common;

import com.mongodb.client.model.Filters;
import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.data.AsylumPlayer;
import eu.asylum.common.data.Rank;
import eu.asylum.common.database.AsylumDB;
import eu.asylum.common.mongoserializer.MongoSerializer;
import eu.asylum.common.utils.Constants;
import eu.asylum.common.utils.TaskWaiter;
import io.lettuce.core.RedisURI;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AsylumProvider<T> {

    private final Map<T, AsylumPlayer<T>> asylumPlayerMap = new ConcurrentHashMap<>();
    private final Map<String, TaskWaiter> uuidWaiterMap = new ConcurrentHashMap<>();
    @Getter
    private final ConfigurationContainer<?> configurationContainer;

    @Getter
    private final AsylumDB asylumDB;
    private final Object lock = new Object();
    private ServerRepository repository = null;

    public AsylumProvider(@NonNull ConfigurationContainer<?> configurationContainer) {
        this.configurationContainer = configurationContainer;
        AsylumConfiguration.setConfigurationContainer(this.configurationContainer);
        RedisURI redisURI = RedisURI.create(AsylumConfiguration.REDIS_URI.getString());
        redisURI.setDatabase(0);
        this.asylumDB = new AsylumDB(redisURI, AsylumConfiguration.MONGODB_URI.getString());
        getOnlinePlayers().forEach(this::getAsylumPlayerAsync);
    }

    /**
     * Return AsylumPlayer
     *
     * @param t return an Optional<AsylumPlayer> given the t object
     **/
    public Optional<AsylumPlayer<T>> getAsylumPlayer(@NonNull T t) {
        synchronized (lock) {
            var ap = this.asylumPlayerMap.get(t);
            if (ap == null) { // fetch from the database, the player is not in the cache

                TaskWaiter waiter = uuidWaiterMap.get(getUsername(t).toLowerCase());

                if (waiter == null) {
                    waiter = new TaskWaiter();

                    uuidWaiterMap.put(getUsername(t).toLowerCase(), waiter);

                    var collection = this.asylumDB.getMongoCollection("asylum", "users");
                    var d = collection.find(Filters.eq("_id", getUUID(t).toString())).first();

                    if (d != null) { // check if a document already exist into the collection.
                        var tempAP = MongoSerializer.deserialize(d, AsylumPlayer.class); // temp AsylumPlayer<Object>
                        ap = new AsylumPlayer<>(tempAP, t); // recreate with the generic.
                    } else { // document not present, creating it.
                        ap = new AsylumPlayer<>(this.getUUID(t), this.getUsername(t), t);
                        ap.setRank(Rank.DEFAULT);
                        collection.insertOne(MongoSerializer.serialize(ap)); // insert into db
                    }

                    if (isOnline(t)) { // fix zombie object caused by player that join and leave fast
                        this.asylumPlayerMap.put(t, ap);
                    }
                    this.uuidWaiterMap.remove(getUsername(t).toLowerCase());
                    waiter.finish();
                    return Optional.of(ap);
                }
                waiter.await(700L); // wait max 700ms
                return Optional.ofNullable(this.asylumPlayerMap.get(t));
            }
            return Optional.of(ap);
        }
    }

    /**
     * Return AsylumPlayer Async
     *
     * @param t return an CompletableFuture<Optional<AsylumPlayer>> given the t object
     **/
    public CompletableFuture<Optional<AsylumPlayer<T>>> getAsylumPlayerAsync(@NonNull T t) {
        return CompletableFuture.supplyAsync(() -> getAsylumPlayer(t), Constants.get().getExecutor());
    }

    /**
     * @param asylumPlayer Update AsylumPlayer data in the database
     **/
    public void saveAsylumPlayer(@NonNull AsylumPlayer<T> asylumPlayer) {
        var data = MongoSerializer.serialize(asylumPlayer);
        var collection = this.asylumDB.getMongoCollection("asylum", "users");
        collection.findOneAndReplace(Filters.eq("_id", asylumPlayer.getUuid().toString()), data);
    }

    /**
     * @param asylumPlayer Update AsylumPlayer data in the database Async
     * @return
     */
    public CompletableFuture<Class<Void>> saveAsylumPlayerAsync(@NonNull AsylumPlayer<T> asylumPlayer) {
        return CompletableFuture.supplyAsync(() -> {
            saveAsylumPlayer(asylumPlayer);
            return Void.TYPE;
        }, Constants.get().getExecutor());
    }

    /**
     * @param ap return the player given an instance of AsylumPlayer
     */
    public Optional<T> getPlayer(@NonNull AsylumPlayer<T> ap) {
        for (var t : this.getOnlinePlayers()) {
            if (this.getUsername(t).equals(ap.getUsername()) && getUUID(t).equals(ap.getUuid())) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }


    /**
     * Get online AsylumPlayers
     */
    public List<Optional<AsylumPlayer<T>>> getOnlineAsylumPlayers() {
        return this.getOnlinePlayers().stream().map(this::getAsylumPlayer).filter(Optional::isPresent).toList();
    }

    /**
     * Get online T and AsylumPlayer TupleList
     */
    public List<ImmutablePair<T, AsylumPlayer<T>>> getPlayersAndAsylumPlayers() {
        List<ImmutablePair<T, AsylumPlayer<T>>> list = new ArrayList<>();
        for (var onlinePlayer : this.getOnlinePlayers()) {
            this.getAsylumPlayer(onlinePlayer).ifPresent(asylumPlayer -> list.add(new ImmutablePair<>(onlinePlayer, asylumPlayer)));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * @param ap return if an AsylumPlayer is online or not
     */
    public boolean isOnline(@NonNull AsylumPlayer<T> ap) {
        var optionalT = getPlayer(ap);
        return optionalT.isPresent() && this.isOnline(optionalT.get()); // if The optional is empty the player is not online
    }

    /**
     * Get if player is online
     */
    public abstract boolean isOnline(@NonNull T t);

    public void onJoin(@NonNull T t) {
        // setup player data
        this.getAsylumPlayerAsync(t);
    }

    public void onQuit(@NonNull T t) {
        // save the data and quit
        this.getAsylumPlayer(t).ifPresent(this::saveAsylumPlayerAsync);
        synchronized (lock) {
            this.asylumPlayerMap.remove(t);
        }
    }

    public final void shutdown() {
        this.asylumDB.shutdown();
        Constants.get().shutdown();
    }

    @NonNull
    public ServerRepository serverRepositoryBuilder() {
        return new ServerRepository(AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
    }

    @Synchronized
    public ServerRepository getRepository() {
        if (repository == null) { // lazy init, initialized ony if needed
            this.repository = serverRepositoryBuilder();
        }
        return repository;
    }

    // Abstract Methods

    /**
     * Get online players
     */
    public abstract List<T> getOnlinePlayers();

    /**
     * Get uuid from player
     */
    public abstract UUID getUUID(@NonNull T t);

    /**
     * Get username from player
     */
    public abstract String getUsername(@NonNull T t);

    /**
     * Send message to a player, message should be already color formatted
     */
    public abstract void sendMessage(@NonNull T t, String message);

    /**
     * Send Actionbar to a player, message should be already color formatted
     */
    public abstract void sendActionBar(@NonNull T t, String message);

    /**
     * Send title to a player, message should be already color formatted
     */
    public abstract void sendTitle(@NonNull T t, String title, String subtitle, int fadeIn, int stay, int fadeOut);

}
