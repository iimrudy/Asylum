package eu.asylum.common.cloud.redis;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.servers.Server;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RedisAsylumServerAdd extends RedisAsylumServer {

    @SerializedName("server")
    private Server server;

}
