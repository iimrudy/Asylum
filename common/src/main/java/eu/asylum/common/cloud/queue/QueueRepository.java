package eu.asylum.common.cloud.queue;

import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.enums.CloudChannels;
import eu.asylum.common.cloud.enums.QueueChannels;
import eu.asylum.common.cloud.enums.QueueLeftReason;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueConnect;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueJoin;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueLeft;
import eu.asylum.common.utils.Constants;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import lombok.Getter;
import lombok.Synchronized;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueRepository extends RedisPubSubAdapter<String, String> {

    @Getter
    protected final ServerRepository repository;
    @Getter
    protected Map<ServerType, Queue<String>> queues = new ConcurrentHashMap<>();
    protected Object _lock = new Object();

    public QueueRepository(ServerRepository repository) {
        this.repository = repository;
        for (ServerType serverType : ServerType.values()) {
            queues.put(serverType, new ConcurrentLinkedQueue<>());
        }
        for (QueueChannels channels : QueueChannels.values()) {
            this.repository.getAsylumDB().getPubSubConnectionReceiver().sync().subscribe(channels.getChannel());
        }
        this.repository.getAsylumDB().getPubSubConnectionReceiver().addListener(this);
    }

    @Override
    @Synchronized
    public void message(String channel, String message) {
        if (channel.equals(QueueChannels.PLAYER_QUEUE_JOIN.getChannel())) {
            var queueJoin = Constants.get().getGson().fromJson(message, RedisQueueJoin.class);
            _addPlayerToQueue(queueJoin.getPlayerName(), queueJoin.getServerType());
            //System.out.println("Player " + queueJoin.getPlayerName() + " joined queue " + queueJoin.getServerType());
        } else if (channel.equals(QueueChannels.PLAYER_QUEUE_LEAVE.getChannel())) {
            var queueJoin = Constants.get().getGson().fromJson(message, RedisQueueLeft.class);
            _removePlayerFromQueues(queueJoin.getPlayerName());
            //System.out.println("Player " + queueJoin.getPlayerName() + " left queue " + queueJoin);
        } else if (channel.equals(QueueChannels.QUEUE_CONNECT.getChannel())) {
            var queueConnect = Constants.get().getGson().fromJson(message, RedisQueueConnect.class);
            this.onConnect(queueConnect);
        }
    }

    public void onConnect(RedisQueueConnect queueConnect) {
    }

    // method called only by redis pubsub handler
    protected void _addPlayerToQueue(String playerName, ServerType serverType) {
        _removePlayerFromQueues(playerName); // replace queue if player is already in queue
        queues.get(serverType).add(playerName);
    }

    // method called only by redis pubsub handler
    protected void _removePlayerFromQueues(String playerName) {
        for (ServerType serverType : ServerType.values()) {
            queues.get(serverType).remove(playerName);
        }
    }

    public void joinQueue(String playerName, ServerType serverType) {
        var queueJoin = new RedisQueueJoin();
        queueJoin.setPlayerName(playerName);
        queueJoin.setServerType(serverType);
        repository.getAsylumDB().publishJson(QueueChannels.PLAYER_QUEUE_JOIN.getChannel(), queueJoin);
    }

    public void leaveQueue(String playerName, QueueLeftReason reason) {
        var queueLeft = new RedisQueueLeft();
        queueLeft.setPlayerName(playerName);
        queueLeft.setReason(reason);
        repository.getAsylumDB().publishJson(QueueChannels.PLAYER_QUEUE_LEAVE.getChannel(), queueLeft);
    }

    public void leaveQueue(String playerName) {
        this.leaveQueue(playerName, QueueLeftReason.LEFT);
    }

    public Optional<ServerType> getPlayerQueue(String username) {
        synchronized (_lock) {
            for (var entry : queues.entrySet()) {
                if (entry.getValue().contains(username)) {
                    return Optional.ofNullable(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

}
