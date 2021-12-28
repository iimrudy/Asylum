package eu.asylum.common.cloud.redis;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RedisAsylumServerMessage extends RedisAsylumServer {

    @SerializedName("message")
    private String message;

}
