package eu.asylum.common.cloud.redis;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RedisAsylumServer {

    @SerializedName("name")
    protected String serverName;

}
