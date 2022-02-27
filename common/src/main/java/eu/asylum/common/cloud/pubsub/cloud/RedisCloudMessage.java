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
public class RedisCloudMessage extends RedisCloudBase {

  @SerializedName("message")
  private String message; // any kind of message, json, simple string & so on
}
