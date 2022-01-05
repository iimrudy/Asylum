package eu.asylum.common.cloud.servers;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudUpdate;
import eu.asylum.common.mongoserializer.annotation.Exclude;
import lombok.Data;

import java.io.Serializable;

@Data
public class Server implements Serializable {

    @Exclude
    private final Object _lock = new Object();

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

    @Exclude
    protected RedisCloudUpdate serverStatus = new RedisCloudUpdate(); // server status

    @Exclude
    protected Pinger pinger;


    public Pinger getPinger() {
        synchronized (_lock) {
            if (this.pinger == null) {
                this.pinger = new Pinger(this.ip, this.port);
            }
            if (!this.pinger.getAddress().equals(this.ip) || this.pinger.getPort() != this.port) { // pinger fixer
                this.pinger.setAddress(this.ip);
                this.pinger.setPort(this.port);
            }
            return this.pinger;
        }
    }

}
