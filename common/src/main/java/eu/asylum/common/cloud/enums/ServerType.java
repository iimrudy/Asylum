package eu.asylum.common.cloud.enums;

import eu.asylum.common.cloud.servers.Server;
import lombok.Getter;

public enum ServerType {

    LOBBY("lobby.zip", 60, 512, 1024, false),
    CAKEWARS_DUOS("cakewars_duos.zip", 15, 512, 1024, false),
    CAKEWARS_SQUAD("cakewars_squad.zip", 20, 512, 1024, false),
    ;

    @Getter
    private final int maxRam;
    @Getter
    private final String zipFile;
    @Getter
    private final int maxPlayers;
    @Getter
    private final int minRam;
    @Getter
    private final boolean isPersistent;


    ServerType(String zipFile, int maxPlayers, int minRam, int maxRam, boolean isPersistent) {
        this.zipFile = zipFile;
        this.maxPlayers = maxPlayers;
        this.minRam = minRam;
        this.maxRam = maxRam;
        this.isPersistent = isPersistent;
    }

    public Server createServer(String name, String host, int port) {
        Server server = new Server();
        server.setServerType(this);
        server.setMinRam(minRam);
        server.setMaxRam(maxRam);
        server.setIp(host);
        server.setPort(port);
        server.setName(name);
        return server;
    }

}
