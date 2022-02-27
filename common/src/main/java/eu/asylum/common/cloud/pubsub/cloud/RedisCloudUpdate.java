package eu.asylum.common.cloud.pubsub.cloud;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
