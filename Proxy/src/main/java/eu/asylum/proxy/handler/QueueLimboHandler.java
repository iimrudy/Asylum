package eu.asylum.proxy.handler;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.asylum.proxy.Proxy;
import lombok.Getter;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class QueueLimboHandler implements LimboSessionHandler {

    @Getter
    private LimboPlayer player;

    private final BossBar bossBar = BossBar.bossBar(MiniMessage.get().parse("<bold><gradient:#95FCFE:#C089F0>SEARCHING FOR A LOBBY SERVER</gradient></bold>"), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.player = player;
        this.player.disableFalling();
        Proxy.get().registerQueueLimbo(player.getProxyPlayer().getUsername(), this);
        player.getProxyPlayer().showBossBar(bossBar);
    }

    @Override
    public void onDisconnect() {
        this.player.getProxyPlayer().hideBossBar(bossBar);
        Proxy.get().unregisterQueueLimbo(player.getProxyPlayer().getUsername());
        Proxy.get().getQueueRepository().leaveQueue(player.getProxyPlayer().getUsername());
    }
}
