package eu.asylum.lobby.configuration;

import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.configuration.IConfigurationEnum;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public enum LobbyConfiguration implements IConfigurationEnum {

    HUB_SPAWN("hub", new Location(Bukkit.getWorlds().get(0), 0, 0, 0)),
    ;

    @Getter
    private final String key;
    @Getter
    private final Object defaultValue;

    LobbyConfiguration(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public Location getLocation(ConfigurationContainer cc) {
        return (Location) cc.get(getKey(), Location.class, ((Location) getDefaultValue()));
    }

    /*@Override
    public void set(ConfigurationContainer cc, Object value) {
        if (value instanceof Location) {
            cc.setKey(getKey() + ".world", ((Location) value).getWorld().getName());
            cc.setKey(getKey() + ".x", ((Location) value).getX());
            cc.setKey(getKey() + ".y", ((Location) value).getY());
            cc.setKey(getKey() + ".z", ((Location) value).getZ());
            cc.setKey(getKey() + ".yaw", ((Location) value).getYaw());
            cc.setKey(getKey() + ".pitch", ((Location) value).getPitch());
        } else {
            ConfigurationEnum.super.set(cc, value);
        }
    }*/
}
