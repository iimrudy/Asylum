package eu.asylum.proxy;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import eu.asylum.common.AsylumProvider;
import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.configuration.ConfigurationContainer;
import lombok.NonNull;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProxyAsylumProvider extends AsylumProvider<Player> {

    public ProxyAsylumProvider(@NonNull ConfigurationContainer<?> configurationContainer) {
        super(configurationContainer);
    }

    @Override
    public boolean isOnline(@NonNull Player player) {
        return player.isActive();
    }

    @Override
    public List<Player> getOnlinePlayers() {
        return new ArrayList<>(Proxy.get().getServer().getAllPlayers());
    }

    @Override
    public UUID getUUID(@NonNull Player player) {
        return player.getUniqueId();
    }

    @Override
    public String getUsername(@NonNull Player player) {
        return player.getUsername();
    }

    @Override
    public void sendMessage(@NonNull Player player, String message) {
        player.sendMessage(MiniMessage.get().parse(message));
    }

    @Override
    public void sendActionBar(@NonNull Player player, String message) {
        player.sendActionBar(MiniMessage.get().parse(message));
    }

    @Override
    public void sendTitle(@NonNull Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        var titleComponent = MiniMessage.get().parse(title);
        var subtitleComponent = MiniMessage.get().parse(subtitle);
        var time = Title.Times.of(Ticks.duration(fadeIn), Ticks.duration(stay), Ticks.duration(fadeOut));
        var t = Title.title(titleComponent, subtitleComponent, time);
        player.showTitle(t);
    }

    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        onJoin(event.getPlayer());
    }

    @Subscribe
    public void onDisconnectEvent(DisconnectEvent event) {
        onQuit(event.getPlayer());
    }

    @Override
    public ServerRepository serverRepositoryBuilder() {
        return new ProxyServerRepository(AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString());
    }
}
