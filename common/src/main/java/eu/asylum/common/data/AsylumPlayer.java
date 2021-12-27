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
    private long firstLogin;

    @SerializedName(value = "rank")
    private Rank rank;

    public AsylumPlayer(@NonNull UUID uuid, @NonNull String username, @NonNull T playerObject) {
        this.uuid = uuid;
        this.username = username;
        this.playerObject = playerObject;
        this.firstLogin = System.currentTimeMillis();
    }

    public AsylumPlayer(@NonNull AsylumPlayer<?> copy, @NonNull T playerObject) {
        this.uuid = copy.uuid;
        this.username = copy.username;
        this.playerObject = playerObject;
        this.firstLogin = copy.firstLogin;
        this.rank = copy.rank;
    }

    public AsylumPlayer() {
        this.rank = Rank.DEFAULT;
        this.username = "";
        this.firstLogin = 0;
        this.playerObject = null;
        this.uuid = null;
    }

}
