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
        System.out.println("ProxyQueueRepository.onConnect() " + queueConnect);
        var optionalServer = Proxy.get().getServer().getServer(queueConnect.getServerName());
        var optionalQueue = Proxy.get().getQueueLimboHandler(queueConnect.getPlayerName());
        var optionalPlayer = Proxy.get().getServer().getPlayer(queueConnect.getPlayerName());
        if (optionalServer.isPresent()) {
            if (optionalQueue.isPresent()) {
                Proxy.get().getQueuedJoin().put(queueConnect.getPlayerName(), optionalServer.get());
                optionalQueue.get().getPlayer().disconnect();
            } else if (optionalPlayer.isPresent()) {
                Proxy.get().getServer().getScheduler().buildTask(Proxy.get(),() -> {
                    optionalPlayer.get().createConnectionRequest(optionalServer.get()).fireAndForget();
                }).schedule();
            }
        }
    }

}
