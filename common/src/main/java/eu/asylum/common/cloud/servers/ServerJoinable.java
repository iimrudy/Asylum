package eu.asylum.common.cloud.servers;

import lombok.NonNull;

public interface ServerJoinable {

    ServerJoinable DEFAULT = (server) -> server.getServerStatus() != null && server.getServerStatus().getOnlinePlayers() < server.getMaxPlayers();

    boolean isJoinable(@NonNull Server server);

}
