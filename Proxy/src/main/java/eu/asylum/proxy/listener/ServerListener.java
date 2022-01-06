package eu.asylum.proxy.listener;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.*;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.proxy.Proxy;
import eu.asylum.proxy.handler.QueueLimboHandler;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;

import java.util.concurrent.TimeUnit;

public class ServerListener {

    private static final LegacyChannelIdentifier LEGACY_BUNGEE_CHANNEL = new LegacyChannelIdentifier("BungeeCord");
    private static final MinecraftChannelIdentifier MODERN_BUNGEE_CHANNEL = MinecraftChannelIdentifier.create("bungeecord", "main");

    @Subscribe
    public void onJoin(LoginEvent event) {
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
        } else {
            if (event.getOriginalServer().getServerInfo().getName().equalsIgnoreCase("lobby")) {
                Proxy.get().getQueueRepository().joinQueue(event.getPlayer().getUsername(), ServerType.LOBBY);
                Proxy.get().getQueueServer().spawnPlayer(event.getPlayer(), new QueueLimboHandler());
            }
        }
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
                System.out.println(playerName + " Connecting to server: " + serverName);
                Proxy.get().getServer().getPlayer(playerName).ifPresent(player -> {
                    Proxy.get().getServer().getServer(serverName).ifPresent(server -> {
                        System.out.println("Connecting to server " + serverName + "  " + playerName);
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
