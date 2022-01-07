package eu.asylum.cloud;

import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.enums.QueueChannels;
import eu.asylum.common.cloud.enums.QueueLeftReason;
import eu.asylum.common.cloud.pubsub.queue.RedisQueueConnect;
import eu.asylum.common.cloud.queue.QueueRepository;
import eu.asylum.common.cloud.servers.Server;
import eu.asylum.common.utils.Constants;
import org.hydev.logger.HyLogger;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static eu.asylum.common.cloud.ServerRepository.LAGGY_TPS;

public class QueueManager extends QueueRepository {

    private final HyLogger logger = new HyLogger("QueueManager");

    public QueueManager(ServerRepository serverRepository) {
        super(serverRepository);
        Constants.get().getExecutor().scheduleAtFixedRate(this::logic, 0, 3, TimeUnit.SECONDS);
    }

    private void logic() {
        synchronized (this.lock) {
            for (var entry : this.queues.entrySet()) { // loop through all queues
                var queue = entry.getValue();
                var serverType = entry.getKey();
                var servers = new ArrayList<>(this.repository.getServers(serverType));
                for (Server server : servers) { // loop through all servers selected by type
                    if (server.getServerStatus().getTps() >= LAGGY_TPS && server.getPinger().ping()) { // join non laggy server & reachable server
                        int onlineCounter = server.getServerStatus().getOnlinePlayers(); // fix overflow players count
                        while (server.getServerType().canJoin(server) && !queue.isEmpty() && onlineCounter < server.getServerType().getMaxPlayers()) { // join until queue is empty
                            var username = queue.poll();
                            // connect player to the server
                            var connectRequest = new RedisQueueConnect();
                            connectRequest.setPlayerName(username);
                            connectRequest.setServerName(server.getName());
                            connectRequest.setServerType(server.getServerType());
                            this.leaveQueue(username, QueueLeftReason.CONNECTED);
                            this.getRepository().getAsylumDB().publishJson(QueueChannels.QUEUE_CONNECT.getChannel(), connectRequest);
                            onlineCounter++;
                            logger.log("Connected " + username + " to " + server.getName());
                        }
                    }
                }
            }
        }
    }


}
