package eu.asylum.proxy.commands;

import static eu.asylum.proxy.Proxy.serialize;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.asylum.common.player.AbstractAsylumPlayer;
import eu.asylum.common.punishments.PunishmentType;
import eu.asylum.proxy.Proxy;
import java.util.Optional;

public class PunishmentCommand implements SimpleCommand {

  @Override
  public void execute(Invocation invocation) {
    if (!hasPermission(invocation)) return;
    long duration = -1;
    PunishmentType punishmentType;
    String reason;

    AbstractAsylumPlayer<Player> issuer =
        invocation.source().equals(Proxy.get().getServer().getConsoleCommandSource())
            ? null
            : Proxy.get().getAsylumProvider().getAsylumPlayer((Player) invocation.source()).get();

    // playerTarget reason
    if (invocation.alias().equalsIgnoreCase("ban")) {
      punishmentType = PunishmentType.BAN;
      this.processPermanent(issuer, punishmentType, invocation.arguments());
    }
    if (invocation.alias().equalsIgnoreCase("kick")) {
      punishmentType = PunishmentType.KICK;
      this.processPermanent(issuer, punishmentType, invocation.arguments());
    }

    // playerTarget reason
    if (invocation.alias().equalsIgnoreCase("mute")) {
      punishmentType = PunishmentType.MUTE;
      this.processPermanent(issuer, punishmentType, invocation.arguments());
    }

    // playerTarget duration reason
    if (invocation.alias().equalsIgnoreCase("tempban")) {
      punishmentType = PunishmentType.BAN;
      return;
    }

    // playerTarget duration reason
    if (invocation.alias().equalsIgnoreCase("tempmute")) {
      punishmentType = PunishmentType.MUTE;
      return;
    }
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    if (invocation.source() instanceof Player) {
      Optional<AbstractAsylumPlayer<Player>> player =
          Proxy.get().getAsylumProvider().getAsylumPlayer((Player) invocation.source());
      return player.isPresent() && player.get().getPlayerData().getRank().isStaff();
    }
    return invocation.source().equals(Proxy.get().getServer().getConsoleCommandSource());
  }

  private void processPermanent(
      AbstractAsylumPlayer<Player> issuer, PunishmentType punishmentType, String... args) {
    if (args.length >= 1) {
      var oPlayer = Proxy.get().getServer().getPlayer(args[0]);
      if (oPlayer.isPresent()) {
        Optional<AbstractAsylumPlayer<Player>> oaap =
            Proxy.get().getAsylumProvider().getAsylumPlayer(oPlayer.get());
        if (oaap.isPresent()) {
          AbstractAsylumPlayer<Player> player = oaap.get();
          String reason = "Unknown Reason";
          if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
              reasonBuilder.append(args[i]).append(" ");
            }
            reason =
                reasonBuilder.substring(0, reasonBuilder.length() - 1); // get string without last space
          }
          Proxy.get()
              .getAsylumProvider()
              .getPunishmentManager()
              .createPunishment(punishmentType, player, issuer, reason, -1);
          if (issuer == null) {
            Proxy.get().getServer().getConsoleCommandSource().sendMessage(serialize.apply("&a" + punishmentType.name() + " created for &b" + player.getUsername()+ "&a." + "&7Reason: &b" + reason + "&7." + "&7Duration: &bPermanent&7." ));
          } else {
            issuer.sendMessage(serialize.apply("&a" + punishmentType.name() + " created for &b" + player.getUsername()+ "&a." + "&7Reason: &b" + reason + "&7." + "&7Duration: &bPermanent&7." ));
          }

          if (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.KICK) {
            player.disconnect(serialize.apply(reason));
          }
        } else {
          // TODO: send message to issuer that server failed to fetch player's data
        }
      } else {
        // TODO: send message to issuer that player is not online
      }
    }
  }
}
