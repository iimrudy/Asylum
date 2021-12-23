package eu.asylum.common.data;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.mongoserializer.annotation.Exclude;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class AsylumPlayer<T> {

    @Exclude
    private T playerObject;
    @SerializedName(value = "username")
    private String username;
    @SerializedName(value = "_id")
    private UUID uuid;
    @SerializedName(value = "firstLogin")
    private long firstLogin = 0;

    @SerializedName(value = "rank")
    private Rank rank = Rank.DEFAULT;

    public AsylumPlayer(@NonNull UUID uuid, @NonNull String username, @NonNull T playerObject) {
        this.uuid = uuid;
        this.username = username;
        this.playerObject = playerObject;
        this.firstLogin = System.currentTimeMillis();
    }

}
