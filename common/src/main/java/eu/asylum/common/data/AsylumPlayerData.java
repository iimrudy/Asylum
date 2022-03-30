package eu.asylum.common.data;

import com.google.gson.annotations.SerializedName;
import eu.asylum.common.punishments.Punishment;
import eu.asylum.common.punishments.PunishmentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;

@Data
public class AsylumPlayerData {

  @SerializedName(value = "username")
  private String username;

  @SerializedName(value = "_id")
  private UUID uuid;

  @SerializedName(value = "firstLogin")
  private long firstLogin;

  @SerializedName(value = "lastLogin")
  private long lastLogin;

  @SerializedName(value = "rank")
  private Rank rank;

  @SerializedName(value = "punishments")
  private List<Punishment> punishmentList;

  public AsylumPlayerData(@NonNull UUID uuid, @NonNull String username) {
    this.uuid = uuid;
    this.username = username;
    this.firstLogin = this.lastLogin = System.currentTimeMillis();
    this.rank = Rank.DEFAULT;
    this.punishmentList = new ArrayList<>();
  }

  public AsylumPlayerData() { // default player data
    this.rank = Rank.DEFAULT;
    this.username = "";
    this.firstLogin = 0;
    this.uuid = null;
  }

  public List<Punishment> getPunishmentList() {
    if (this.punishmentList == null) {
      this.punishmentList = new ArrayList<>();
    }
    if (!(this.punishmentList
        instanceof ArrayList)) { // make sure it's an arraylist, so we can edit the list
      this.punishmentList = new ArrayList<>(this.punishmentList);
    }
    return punishmentList;
  }

  public Optional<Punishment> hasPunishmentActive(PunishmentType type) {
    return this.getPunishmentList().stream()
        .filter(punishment -> punishment.getType() == type)
        .filter(Punishment::isApplicable)
        .findFirst();
  }
}
