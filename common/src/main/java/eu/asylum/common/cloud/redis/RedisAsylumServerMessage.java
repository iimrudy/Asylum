package eu.asylum.common.cloud.redis;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RedisAsylumServerMessage extends RedisAsylumServer {

    @SerializedName("message")
    private String message; // any kind of message, json, simple string & so on

}
