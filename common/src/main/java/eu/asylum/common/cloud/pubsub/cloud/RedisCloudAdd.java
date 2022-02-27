package eu.asylum.common.cloud.pubsub.cloud;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.servers.Server;
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
public class RedisCloudAdd extends RedisCloudBase {

  @SerializedName("server")
  private Server server;
}
