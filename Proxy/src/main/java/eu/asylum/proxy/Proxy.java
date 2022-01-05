package eu.asylum.proxy;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.asylum.common.AsylumProvider;
import eu.asylum.proxy.configuration.TomlConfigurationContainer;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Plugin(id = "proxy", name = "Proxy", version = "1.0-SNAPSHOT", description = "I did it!", authors = {"iim_rudy"})
@Getter
public class Proxy {

    private static Proxy instance;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final AsylumProvider<Player> asylumProvider;

    @Inject
    public Proxy(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        instance = this;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = folder;
        logger.info("Hello there! I made my first plugin with Velocity.");
        this.asylumProvider = new ProxyAsylumProvider(new TomlConfigurationContainer(loadConfig(new File(folder.toFile(), "AsylumCommon.toml"))));
        server.getEventManager().register(this, this.asylumProvider);
    }

    public static Proxy get() {
        return instance;
    }

    @SneakyThrows
    private Toml loadConfig(File file) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        return new Toml().read(file);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Proxy initialized!");
    }

}
