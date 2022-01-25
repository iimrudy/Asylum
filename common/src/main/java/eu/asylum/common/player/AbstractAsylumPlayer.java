package eu.asylum.common.player;

import eu.asylum.common.data.AsylumPlayerData;
import lombok.Data;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

@Data
public abstract class AbstractAsylumPlayer<T> {

    protected final T playerObject;

    private AsylumPlayerData playerData = new AsylumPlayerData();

    public abstract void sendMessage(@NonNull Component component);

    public abstract void sendActionBar(@NonNull Component component);

    public abstract void sendTitle(@NonNull Component title, @NonNull Component subtitle, @NonNull long fadeIn, @NonNull long stay, @NonNull long fadeOut);

    public abstract void disconnect(@NonNull Component component);

    public abstract String getUsername();

    public abstract UUID getUniqueId();

    public abstract boolean isOnline();

    public abstract Optional<InetSocketAddress> getAddress();

}
