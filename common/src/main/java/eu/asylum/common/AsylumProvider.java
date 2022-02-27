package eu.asylum.common;

import com.mongodb.client.model.Filters;
import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.data.AsylumPlayerData;
import eu.asylum.common.database.AsylumDB;
import eu.asylum.common.mongoserializer.MongoSerializer;
import eu.asylum.common.player.AbstractAsylumPlayer;
import eu.asylum.common.punishments.PunishmentManager;
import eu.asylum.common.utils.Constants;
import eu.asylum.common.utils.TaskWaiter;
import io.lettuce.core.RedisURI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import org.apache.commons.lang3.tuple.ImmutablePair;

public abstract class AsylumProvider<T> {

  private final Map<T, AbstractAsylumPlayer<T>> asylumPlayerMap = new ConcurrentHashMap<>();
  private final Map<String, TaskWaiter> uuidWaiterMap = new ConcurrentHashMap<>();
  @Getter private final ConfigurationContainer<?> configurationContainer;

  @Getter private final AsylumDB asylumDB;
  private final Object lock = new Object();
  @Getter private final PunishmentManager punishmentManager;
  private ServerRepository repository = null;

  public AsylumProvider(@NonNull ConfigurationContainer<?> configurationContainer) {
    this.configurationContainer = configurationContainer;
    AsylumConfiguration.setConfigurationContainer(this.configurationContainer);
    RedisURI redisURI = RedisURI.create(AsylumConfiguration.REDIS_URI.getString());
    redisURI.setDatabase(0);
    this.asylumDB = new AsylumDB(redisURI, AsylumConfiguration.MONGODB_URI.getString());
    getOnlinePlayers().forEach(this::getAsylumPlayerAsync);

    this.punishmentManager = new PunishmentManager(this);
  }

  /**
   * Return AsylumPlayer
   *
   * @param t return an Optional<AsylumPlayer> given the t object
   */
  public Optional<AbstractAsylumPlayer<T>> getAsylumPlayer(@NonNull T t) {
    synchronized (lock) {
      var ap = this.asylumPlayerMap.get(t);
      if (ap == null) { // fetch from the database, the player is not in the cache
        ap = craftAsylumPlayer(t);
        TaskWaiter waiter = uuidWaiterMap.get(ap.getUsername());

        if (waiter == null) {
          waiter = new TaskWaiter();

          uuidWaiterMap.put(ap.getUsername(), waiter);

          var collection = this.asylumDB.getMongoCollection("asylum", "users");
          var d = collection.find(Filters.eq("_id", ap.getUniqueId().toString())).first();

          if (d != null) { // check if a document already exist into the collection.
            var tempAP =
                MongoSerializer.deserialize(d, AsylumPlayerData.class); // temp AsylumPlayer<Object>
            ap.setPlayerData(tempAP);
          } else { // document not present, creating it.
            collection.insertOne(MongoSerializer.serialize(ap.getPlayerData())); // insert into db
          }
          ap.getPlayerData().setLastLogin(System.currentTimeMillis());

          if (ap.isOnline()) { // fix zombie object caused by player that join and leave fast
            this.asylumPlayerMap.put(t, ap);
          }
          this.uuidWaiterMap.remove(ap.getUsername());
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
   */
  public CompletableFuture<Optional<AbstractAsylumPlayer<T>>> getAsylumPlayerAsync(@NonNull T t) {
    return CompletableFuture.supplyAsync(() -> getAsylumPlayer(t), Constants.get().getExecutor());
  }

  /**
   * @param asylumPlayer Update AsylumPlayer data in the database
   */
  public void saveAsylumPlayer(@NonNull AbstractAsylumPlayer<T> asylumPlayer) {
    var data = MongoSerializer.serialize(asylumPlayer.getPlayerData());
    var collection = this.asylumDB.getMongoCollection("asylum", "users");
    collection.findOneAndReplace(Filters.eq("_id", asylumPlayer.getUniqueId().toString()), data);
  }

  /**
   * @param asylumPlayer Update AsylumPlayer data in the database Async
   */
  public CompletableFuture<Class<Void>> saveAsylumPlayerAsync(
      @NonNull AbstractAsylumPlayer<T> asylumPlayer) {
    return CompletableFuture.supplyAsync(
        () -> {
          saveAsylumPlayer(asylumPlayer);
          return Void.TYPE;
        },
        Constants.get().getExecutor());
  }

  /** Get online AsylumPlayers */
  public List<Optional<AbstractAsylumPlayer<T>>> getOnlineAsylumPlayers() {
    return this.getOnlinePlayers().stream()
        .map(this::getAsylumPlayer)
        .filter(Optional::isPresent)
        .toList();
  }

  /** Get online T and AsylumPlayer TupleList */
  public List<ImmutablePair<T, AbstractAsylumPlayer<T>>> getPlayersAndAsylumPlayers() {
    List<ImmutablePair<T, AbstractAsylumPlayer<T>>> list = new ArrayList<>();
    for (var onlinePlayer : this.getOnlinePlayers()) {
      this.getAsylumPlayer(onlinePlayer)
          .ifPresent(
              asylumPlayerData -> list.add(new ImmutablePair<>(onlinePlayer, asylumPlayerData)));
    }
    return Collections.unmodifiableList(list);
  }

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
    return new ServerRepository(
        AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
  }

  @Synchronized
  public ServerRepository getRepository() {
    if (repository == null) { // lazy init, initialized only if needed
      this.repository = serverRepositoryBuilder();
    }
    return repository;
  }

  // Abstract Methods

  /** Get online players */
  public abstract List<T> getOnlinePlayers();

  public abstract AbstractAsylumPlayer<T> craftAsylumPlayer(@NonNull T playerObject);
}
