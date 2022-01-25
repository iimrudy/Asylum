package eu.asylum.proxy;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import eu.asylum.common.AsylumProvider;
import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.player.AbstractAsylumPlayer;
import eu.asylum.proxy.player.ProxyAsylumPlayer;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ProxyAsylumProvider extends AsylumProvider<Player> {

    public ProxyAsylumProvider(@NonNull ConfigurationContainer<?> configurationContainer) {
        super(configurationContainer);
    }

    @Override
    public List<Player> getOnlinePlayers() {
        return new ArrayList<>(Proxy.get().getServer().getAllPlayers());
    }

    @Override
    public AbstractAsylumPlayer<Player> craftAsylumPlayer(@NonNull Player playerObject) {
        return new ProxyAsylumPlayer(playerObject);
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
