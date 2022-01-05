package eu.asylum.common.cloud.pubsub.cloud;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.servers.Server;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RedisCloudDelete extends RedisCloudBase {

    @SerializedName("server")
    private Server server;

}
