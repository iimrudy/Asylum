package eu.asylum.lobby.configuration;

import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.configuration.IConfigurationEnum;
import eu.asylum.lobby.AsylumLobby;
import lombok.Getter;
import org.bukkit.Location;

import java.util.Arrays;

public enum LobbyConfiguration implements IConfigurationEnum {

    HUB_SPAWN("hub", new Location(null, 0, 0, 0)),
    SCOREBOARD("scoreboard", Arrays.asList("TITLE", "LINE1", "LINE2")),
    ;

    @Getter
    private final String key;
    @Getter
    private final Object defaultValue;

    LobbyConfiguration(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public final ConfigurationContainer<?> getConfig() {
        return AsylumLobby.getInstance().getConfiguration();
    }
}
