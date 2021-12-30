package eu.asylum.common.cloud.redis;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.StringJoiner;

@Getter
@Setter
public class RedisAsylumServerUpdate extends RedisAsylumServer {

    @SerializedName("ramUsage")
    private long ramUsage;
    @SerializedName("tps")
    private double tps;
    @SerializedName("onlinePlayers")
    private int onlinePlayers;
    @SerializedName("motd")
    private String motd;


    @Override
    public String toString() {
        return new StringJoiner(", ", RedisAsylumServerUpdate.class.getSimpleName() + "[", "]")
                .add("serverName='" + serverName + "'")
                .add("ramUsage=" + ramUsage)
                .add("tps=" + tps)
                .add("onlinePlayers=" + onlinePlayers)
                .add("motd=" + motd)
                .toString();
    }
}
