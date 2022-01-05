package eu.asylum.proxy;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import eu.asylum.common.cloud.ServerRepository;
import eu.asylum.common.cloud.servers.Server;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyServerRepository extends ServerRepository {

    private final Map<Server, RegisteredServer> proxyServerMap = new ConcurrentHashMap<>();

    public ProxyServerRepository(String redisUri, String mongoUri) {
        super(redisUri, mongoUri);
    }

    @Override
    public void onServerAdd(@NonNull Server server) {
        if (!proxyServerMap.containsKey(server)) {
            var info = new ServerInfo(server.getName(), new InetSocketAddress(server.getIp(), server.getPort()));
            RegisteredServer rs = Proxy.get().getServer().registerServer(info);
            proxyServerMap.put(server, rs);
        }
    }

    @Override
    public void onServerDelete(@NonNull Server server) {
        var registeredServer = proxyServerMap.get(server);
        if (registeredServer != null)
            Proxy.get().getServer().unregisterServer(registeredServer.getServerInfo());
    }

    @Override
    public void onServerUpdate(@NonNull Server server) {
    }

    @Override
    public void onSync() {
        var servers = new ArrayList<>(getServers());

        // register new servers
        for (Server server : servers) {
            if (!this.proxyServerMap.containsKey(server)) {
                var info = new ServerInfo(server.getName(), new InetSocketAddress(server.getIp(), server.getPort()));
                RegisteredServer rs = Proxy.get().getServer().registerServer(info);
                proxyServerMap.put(server, rs);
                Proxy.get().getLogger().info("Registering a new server #-> " + server.getName() + "\t|" + this.proxyServerMap.size());
            }
        }

        // unregister dead servers
        for (var server2 : this.proxyServerMap.entrySet()) {
            if (!servers.contains(server2.getKey())) {
                Proxy.get().getServer().unregisterServer(server2.getValue().getServerInfo());
                this.proxyServerMap.remove(server2.getKey(), server2.getValue());
                Proxy.get().getLogger().info("Unregistered a server #-> " + server2.getKey().getName() + "\t|" + this.proxyServerMap.size());
            }
        }
    }
}
