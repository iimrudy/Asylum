package eu.asylum.proxy.handler;

import eu.asylum.common.cloud.enums.ServerType;
import eu.asylum.proxy.Proxy;
import eu.asylum.proxy.listener.ServerListener;
import lombok.Getter;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class QueueLimboHandler implements LimboSessionHandler {

  private final BossBar bossBar =
      BossBar.bossBar(
          MiniMessage.get()
              .parse(
                  "<bold><gradient:#95FCFE:#C089F0>SEARCHING FOR A LOBBY SERVER</gradient></bold>"),
          1,
          BossBar.Color.BLUE,
          BossBar.Overlay.PROGRESS);
  @Getter private LimboPlayer player;

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.player = player;
    this.player.disableFalling();

    player.getProxyPlayer().showBossBar(bossBar);

    if (!ServerListener.checkPunishments(player.getProxyPlayer())) {
      Proxy.get().registerQueueLimbo(player.getProxyPlayer().getUsername(), this);
      Proxy.get()
          .getQueueRepository()
          .joinQueue(player.getProxyPlayer().getUsername(), ServerType.LOBBY);
    }
  }

  @Override
  public void onDisconnect() {
    this.player.getProxyPlayer().hideBossBar(bossBar);
    Proxy.get().getAsylumProvider().onQuit(player.getProxyPlayer());

    Proxy.get().unregisterQueueLimbo(player.getProxyPlayer().getUsername());
    Proxy.get().getQueueRepository().leaveQueue(player.getProxyPlayer().getUsername());
  }
}
