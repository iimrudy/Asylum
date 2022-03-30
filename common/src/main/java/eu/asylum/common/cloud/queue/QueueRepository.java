package eu.asylum.common.cloud.queue;

import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.enums.QueueChannels;
import eu.asylum.common.cloud.enums.QueueLeftReason;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueConnect;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueJoin;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueLeft;
import eu.asylum.common.database.AsylumDB;
import eu.asylum.common.utils.Constants;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.Synchronized;

public class QueueRepository extends RedisPubSubAdapter<String, String> {

  @Getter protected final ServerRepository repository;
  protected final AsylumDB db;

  public QueueRepository(ServerRepository repository) {
    this.repository = repository;
    this.db = this.repository.getAsylumDB();

    for (QueueChannels channels : QueueChannels.values()) {
      this.db.getPubSubConnectionReceiver().sync().subscribe(channels.getChannel());
    }
    this.db.getPubSubConnectionReceiver().addListener(this);
  }

  @Override
  @Synchronized
  public void message(String channel, String message) {
    if (channel.equals(QueueChannels.PLAYER_QUEUE_JOIN.getChannel())) {
      var queueJoin = Constants.get().getGson().fromJson(message, RedisQueueJoin.class);
      this.onJoinQueue(queueJoin);
    } else if (channel.equals(QueueChannels.PLAYER_QUEUE_LEAVE.getChannel())) {
      var queueLeft = Constants.get().getGson().fromJson(message, RedisQueueLeft.class);
      this.onQueueLeft(queueLeft);
    } else if (channel.equals(QueueChannels.QUEUE_CONNECT.getChannel())) {
      var queueConnect = Constants.get().getGson().fromJson(message, RedisQueueConnect.class);
      this.onConnect(queueConnect);
    }
  }

  public void onConnect(RedisQueueConnect queueConnect) {
    // this method can be overridden by subclasses that need to do something on queue connect
  }

  public void onJoinQueue(RedisQueueJoin queueJoin) {
    // this method can be overridden by subclasses that need to do something on queue join

  }

  public void onQueueLeft(RedisQueueLeft queueLeft) {
    // this method can be overridden by subclasses that need to do something on queue left
  }

  // method called only by redis pubsub handler
  protected CompletableFuture<Void> removePlayerFromQueues(String playerName) {
    return CompletableFuture.supplyAsync(
        () -> {
          for (ServerType serverType : ServerType.values()) {
            this.db.getRedisConnection().sync().srem(serverType.getQueueName(), playerName);
          }
          return null;
        },
        Constants.get().getExecutor());
  }

  public CompletableFuture<Void> joinQueue(String username, ServerType serverType) {
    var finalUsername = username.toLowerCase();
    return removePlayerFromQueues(username)
        .thenApply(
            __ -> {
              this.db.getRedisConnection().sync().sadd(serverType.getQueueName(), finalUsername);
              var x = new RedisQueueJoin();
              x.setPlayerName(finalUsername);
              x.setServerType(serverType);
              this.db.publishJson(QueueChannels.PLAYER_QUEUE_JOIN.getChannel(), x);
              return null;
            });
  }

  public CompletableFuture<Void> leaveQueue(String playerName, QueueLeftReason reason) {
    return removePlayerFromQueues(playerName)
        .thenApply(
            __ -> {
              var queueLeft = new RedisQueueLeft();
              queueLeft.setPlayerName(playerName);
              queueLeft.setReason(reason);
              this.db.publishJson(QueueChannels.PLAYER_QUEUE_LEAVE.getChannel(), queueLeft);
              return null;
            });
  }

  public void leaveQueue(String playerName) {
    this.leaveQueue(playerName, QueueLeftReason.LEFT);
  }

  public CompletableFuture<Optional<ServerType>> getPlayerQueue(String username) {
    var finalUsername = username.toLowerCase();
    return CompletableFuture.supplyAsync(
        () -> {
          for (ServerType serverType : ServerType.values()) {
            if (this.db
                .getRedisConnection()
                .sync()
                .sismember(serverType.getQueueName(), finalUsername)) {
              return Optional.of(serverType);
            }
          }
          return Optional.empty();
        },
        Constants.get().getExecutor());
  }

  public Map<ServerType, Queue<String>> getQueues() {
    HashMap<ServerType, Queue<String>> map = new HashMap<>();
    for (ServerType serverType : ServerType.values()) {
      Queue<String> queues = new LinkedList<>();
      this.db
          .getRedisConnection()
          .sync()
          .sscan(serverType.getQueueName())
          .getValues()
          .forEach(queues::add);
      map.put(serverType, queues);
    }
    return map;
  }
}
