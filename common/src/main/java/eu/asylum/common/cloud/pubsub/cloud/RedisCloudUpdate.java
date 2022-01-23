package eu.asylum.common.cloud.pubsub.cloud;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RedisCloudUpdate extends RedisCloudBase {

    @SerializedName("ramUsage")
    private long ramUsage;
    @SerializedName("tps")
    private double tps;
    @SerializedName("onlinePlayers")
    private int onlinePlayers;
    @SerializedName("motd")
    private String motd;

}
