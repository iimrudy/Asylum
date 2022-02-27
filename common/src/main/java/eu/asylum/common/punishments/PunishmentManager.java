package eu.asylum.common.punishments;

import eu.asylum.common.AsylumProvider;
import eu.asylum.common.player.AbstractAsylumPlayer;
import java.util.Optional;
import lombok.NonNull;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PunishmentManager {

  private final AsylumProvider provider;

  public PunishmentManager(AsylumProvider provider) {
    this.provider = provider;
  }

  public Optional<Punishment> createPunishment(
      @NonNull PunishmentType type,
      @NonNull AbstractAsylumPlayer player,
      AbstractAsylumPlayer issuer,
      @NonNull String reason,
      @NonNull long duration) {
    String issuerName = "Console";
    if (issuer != null) {
      if (!issuer.getPlayerData().getRank().isStaff()) {
        issuer.sendMessage(MiniMessage.get().deserialize("Unknown command."));
        return Optional.empty();
      }
      issuerName = issuer.getUsername();
    }
    Punishment punishment =
        new Punishment(
            type,
            reason,
            System.currentTimeMillis(),
            duration <= 0 ? -1 : duration,
            issuerName,
            player.getUsername(),
            type != PunishmentType.KICK);
    player.getPlayerData().getPunishmentList().add(punishment);
    provider.saveAsylumPlayerAsync(player);
    return Optional.of(punishment);
  }
}
