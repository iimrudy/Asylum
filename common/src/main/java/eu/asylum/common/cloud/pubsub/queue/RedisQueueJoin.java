package eu.asylum.common.cloud.pubsub.queue;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.enums.ServerType;
import lombok.Data;

@Data
public class RedisQueueJoin {

  @SerializedName("PlayerName")
  private String playerName;

  @SerializedName("serverType")
  private ServerType serverType;
}
