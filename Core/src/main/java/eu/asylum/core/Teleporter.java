package eu.asylum.core;

import eu.asylum.common.cloud.enums.ServerType;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Teleporter {

  private static void sendPacketToProxy(String... data) {
    final ByteArrayOutputStream b = new ByteArrayOutputStream();
    final DataOutputStream out = new DataOutputStream(b);
    try {
      for (var d : data) {
        out.writeUTF(d);
      }
      Bukkit.getOnlinePlayers().stream()
          .findFirst()
          .ifPresent(
              player -> {
                player.sendPluginMessage(AsylumCore.getInstance(), "BungeeCord", b.toByteArray());
              });
    } catch (Exception e) {
      e.printStackTrace();
      AsylumCore.getInstance().getLogger().severe("Failed to send packet to proxy!");
    }
  }

  public static void joinQueue(Player player, ServerType serverType) {
    sendPacketToProxy("cloud", "queue:join", serverType.name(), player.getName());
  }

  public static void leaveQueue(Player player) {
    sendPacketToProxy("cloud", "queue:leave", player.getName());
  }

  public static void joinQueueLobby(Player player) {
    joinQueue(player, ServerType.LOBBY);
  }

  public static void connect(Player player, String serverName) {
    sendPacketToProxy("cloud", "connect:server", serverName, player.getName());
  }
}
