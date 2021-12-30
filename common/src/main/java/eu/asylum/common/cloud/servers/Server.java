package eu.asylum.common.cloud.servers;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.redis.RedisAsylumServerUpdate;
import eu.asylum.common.mongoserializer.annotation.Exclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class Server implements Serializable {

    @SerializedName("name")
    protected String name; // server name

    @SerializedName("ip")
    protected String ip; // server ip

    @SerializedName("port")
    protected int port; // server port

    @SerializedName("minRam")
    protected int minRam; // min given ram in MBytes

    @SerializedName("maxRam")
    protected int maxRam; // max given ram in MBytes

    @SerializedName("maxPlayers")
    protected int maxPlayers; // max Players on this server

    @SerializedName("serverType")
    protected ServerType serverType; // server type

    @SerializedName("isPersistent")
    protected boolean isPersistent; // don't delete server on shutdown

    @EqualsAndHashCode.Exclude
    @Exclude
    protected RedisAsylumServerUpdate serverStatus = new RedisAsylumServerUpdate(); // server status

    @EqualsAndHashCode.Exclude
    @Exclude
    protected Pinger pinger;

    @Synchronized
    public Pinger getPinger() {
        if (this.pinger == null) {
            this.pinger = new Pinger(this.ip, this.port);
        }
        if (!this.pinger.getAddress().equals(this.ip) || this.pinger.getPort() != this.port) { // pinger fixer
            this.pinger.setAddress(this.ip);
            this.pinger.setPort(this.port);
        }
        return this.pinger;
    }

    public void copy(Server server) {
        this.ip = server.ip;
        this.port = server.port;
        this.name = server.name;
        this.minRam = server.minRam;
        this.maxRam = server.maxRam;
        this.maxPlayers = server.maxPlayers;
        this.serverType = server.serverType;
        this.isPersistent = server.isPersistent;
    }

}
