package eu.asylum.proxy.listener;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.proxy.Proxy;
import eu.asylum.proxy.handler.QueueLimboHandler;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.function.Function;

public class ServerListener {

    public static final char BAR_CHAR = '\u2588'; // full block character
    public static final char HEAVY_VERTICAL = '\u2503'; // box drawings heavy vertical character
    private static final LegacyChannelIdentifier LEGACY_BUNGEE_CHANNEL = new LegacyChannelIdentifier("BungeeCord");
    private static final MinecraftChannelIdentifier MODERN_BUNGEE_CHANNEL = MinecraftChannelIdentifier.create("bungeecord", "main");

    private static final Function<String, Component> serialize = message -> LegacyComponentSerializer.legacyAmpersand().deserialize(message);

    public ServerListener() {
        Proxy.get().getServer().getChannelRegistrar().register(LEGACY_BUNGEE_CHANNEL, MODERN_BUNGEE_CHANNEL);
    }

    @Subscribe
    public void onJoin(LoginEvent event) {
        // todo
    }

    @Subscribe
    public void disconnect(DisconnectEvent event) {
        Proxy.get().unregisterQueueLimbo(event.getPlayer().getUsername());
        Proxy.get().getQueuedJoin().remove(event.getPlayer().getUsername());
        Proxy.get().getQueueRepository().leaveQueue(event.getPlayer().getUsername());
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        var isQueued = Proxy.get().getQueuedJoin().get(event.getPlayer().getUsername());
        if (isQueued != null) {
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(isQueued));
            Proxy.get().getQueuedJoin().remove(event.getPlayer().getUsername());
        }
    }

    @Subscribe
    public void onPostConnect(ServerConnectedEvent event) {
        var message = "&b&lTeleporter &8" + HEAVY_VERTICAL + " &7Connected to &a" + event.getServer().getServerInfo().getName();
        if (event.getPreviousServer().isPresent()) {
            message += " &7from &6" + event.getPreviousServer().get().getServerInfo().getName();
        }
        event.getPlayer().sendMessage(serialize.apply(message));
    }


    @Subscribe
    public void pluginMessageEvent(final PluginMessageEvent event) {
        if (!event.getIdentifier().equals(LEGACY_BUNGEE_CHANNEL) && !event.getIdentifier().equals(MODERN_BUNGEE_CHANNEL)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        ByteArrayDataInput in = event.dataAsDataStream();
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        String subChannel = in.readUTF();

        if (subChannel.equals("cloud")) {
            String command = in.readUTF();
            if (command.equals("queue:join")) { // join queue for a server
                String serverType = in.readUTF();
                String playerName = in.readUTF();
                try {
                    ServerType type = ServerType.valueOf(serverType);
                    Proxy.get().getQueueRepository().joinQueue(playerName, type);
                } catch (Exception e) {
                    // server type not found
                }
            } else if (command.equals("queue:leave")) { // leave queue for a server
                String playerName = in.readUTF();
                Proxy.get().getQueueRepository().leaveQueue(playerName);
            } else if (command.equals("connect:server")) { // connect to a server
                String serverName = in.readUTF();
                String playerName = in.readUTF();
                Proxy.get().getServer().getPlayer(playerName).ifPresent(player -> {
                    Proxy.get().getServer().getServer(serverName).ifPresent(server -> {
                        var o = Proxy.get().getQueueLimboHandler(player.getUsername());
                        if (o.isPresent()) {
                            o.get().getPlayer().disconnect(server);
                        } else {
                            player.createConnectionRequest(server).fireAndForget();
                        }
                    });
                });
            }
        }
    }

    @Subscribe
    public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
        event.addCallback(() -> {
            Proxy.get().getQueueRepository().joinQueue(event.getPlayer().getUsername(), ServerType.LOBBY);
            Proxy.get().getQueueServer().spawnPlayer(event.getPlayer(), new QueueLimboHandler());
        });
    }
}
