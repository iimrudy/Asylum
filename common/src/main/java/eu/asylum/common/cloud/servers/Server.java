package eu.asylum.common.cloud.servers;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.enums.ServerType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public abstract class Server implements Serializable {

    @SerializedName("name")
    protected String name;

    @SerializedName("ip")
    protected String ip;

    @SerializedName("port")
    protected int port;

    @SerializedName("minRam")
    protected int minRam;

    @SerializedName("maxRam")
    protected int maxRam;

    @SerializedName("onlinePlayers")
    protected int onlinePlayers;

    @SerializedName("maxPlayers")
    protected int maxPlayers;

    @SerializedName("serverType")
    protected ServerType serverType;

    @EqualsAndHashCode.Exclude
    protected Pinger pinger;

    public Pinger pinger() {
        if (pinger == null) {
            pinger = new Pinger(this.ip, this.port);
        }
        if (!pinger.getAddress().equals(this.ip) || pinger.getPort() != this.port) { // pinger fixer
            this.pinger.setAddress(this.ip);
            this.pinger.setPort(this.port);
        }
        return this.pinger;
    }

}
