package eu.asylum.common.cloud.servers;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.pubsub.cloud.RedisCloudUpdate;
import eu.asylum.common.mongoserializer.annotation.Exclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Server {

    @Exclude
    private final Object lock = new Object();

    @SerializedName("name")
    protected String name; // server name

    @SerializedName("ip")
    protected String ip; // server ip

    @SerializedName("port")
    protected int port; // server port

    @SerializedName("serverType")
    protected ServerType serverType; // server type

    @Exclude
    protected RedisCloudUpdate serverStatus = new RedisCloudUpdate(); // server status

    @Exclude
    protected Pinger pinger;


    public Pinger getPinger() {
        synchronized (lock) {
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return getPort() == server.getPort() && Objects.equals(getName(), server.getName()) && Objects.equals(getIp(), server.getIp()) && getServerType() == server.getServerType();
    }

}
