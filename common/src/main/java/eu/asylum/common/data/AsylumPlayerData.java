package eu.asylum.common.data;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NonNull;

import java.util.UUID;

@Data
public class AsylumPlayerData {

    @SerializedName(value = "username")
    private String username;
    @SerializedName(value = "_id")
    private UUID uuid;
    @SerializedName(value = "firstLogin")
    private long firstLogin;

    @SerializedName(value = "rank")
    private Rank rank;

    public AsylumPlayerData(@NonNull UUID uuid, @NonNull String username) {
        this.uuid = uuid;
        this.username = username;
        this.firstLogin = System.currentTimeMillis();
    }

    public AsylumPlayerData() { // default player data
        this.rank = Rank.DEFAULT;
        this.username = "";
        this.firstLogin = 0;
        this.uuid = null;
    }

}
