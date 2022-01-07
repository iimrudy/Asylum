package eu.asylum.proxy;

import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueConnect;
import eu.asylum.common.cloud.queue.QueueRepository;

public class ProxyQueueRepository extends QueueRepository {

    public ProxyQueueRepository(ServerRepository repository) {
        super(repository);
    }

    @Override
    public void onConnect(RedisQueueConnect queueConnect) {
        var optionalServer = Proxy.get().getServer().getServer(queueConnect.getServerName());
        var optionalQueue = Proxy.get().getQueueLimboHandler(queueConnect.getPlayerName());
        var optionalPlayer = Proxy.get().getServer().getPlayer(queueConnect.getPlayerName());
        if (optionalServer.isPresent()) { // if server exist
            if (optionalQueue.isPresent()) { // if the player is in the limbo
                Proxy.get().getQueuedJoin().put(queueConnect.getPlayerName(), optionalServer.get()); // limbo players are connected in a different way
                optionalQueue.get().getPlayer().disconnect();
            } else if (optionalPlayer.isPresent()) { // else just send the player
                Proxy.get().getServer().getScheduler().buildTask(Proxy.get(), () -> {
                    optionalPlayer.get().createConnectionRequest(optionalServer.get()).fireAndForget();
                }).schedule();
            }
        }
    }

}
