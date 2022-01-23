package eu.asylum.common.cloud.pubsub.queue;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.enums.QueueLeftReason;
import lombok.Data;

@Data
public class RedisQueueLeft {

    @SerializedName("PlayerName")
    private String playerName;

    @SerializedName("reason")
    private QueueLeftReason reason;

}
