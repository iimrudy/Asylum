package eu.asylum.common.cloud.servers;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.enums.ServerGamemode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class MinecraftServer extends Server {

    @SerializedName("gamemode")
    protected ServerGamemode serverGamemode;

    @SerializedName("motd")
    protected String motd;

}
