package eu.asylum.common.cloud.pubsub.cloud;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RedisCloudMessage extends RedisCloudBase {

    @SerializedName("message")
    private String message; // any kind of message, json, simple string & so on

}
