package eu.asylum.common.cloud.servers.template;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.cloud.enums.ServerGamemode;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.common.cloud.servers.Server;
import lombok.Getter;

@Getter
public class ServerTemplate extends Server {

    @SerializedName("name")
    private String templateName;
    @SerializedName("path")
    private String templatePath;
    @SerializedName("type")
    private ServerType serverType;
    @SerializedName("gamemode")
    private ServerGamemode serverGamemode;

    @SerializedName("maxPlayers")
    private int maxPlayers;

    @SerializedName("minRam")
    private int minRam;

    @SerializedName("maxRam")
    private int maxRam;

}
