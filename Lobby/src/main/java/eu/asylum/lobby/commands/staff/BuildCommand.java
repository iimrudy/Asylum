package eu.asylum.lobby.commands.staff;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import eu.asylum.core.AsylumCore;
import eu.asylum.core.helpers.MessageUtils;
import eu.asylum.lobby.AsylumLobby;
import eu.asylum.lobby.Items;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@CommandAlias("build")
public class BuildCommand extends BaseCommand {

    @Subcommand("toggle")
    @Syntax("[player]")
    @CommandCompletion("")
    public void toggleBuild(Player sender, @Optional OnlinePlayer target) {
        var apSender = AsylumCore.getInstance().getAsylumProvider().getAsylumPlayer(sender);
        if (apSender.isEmpty() || !apSender.get().getPlayerData().getRank().isOwnerOrAdmin()) return;

        if (target != null && target.getPlayer() != null && target.getPlayer().isOnline()) {
            var apTarget = AsylumCore.getInstance().getAsylumProvider().getAsylumPlayer(target.getPlayer());
            if (apTarget.isEmpty()) return;
            if (!apSender.get().getPlayerData().getRank().isOwnerOrAdmin()) return;
            if (AsylumLobby.getInstance().getBuildingPlayers().contains(target.getPlayer())) {
                AsylumLobby.getInstance().getBuildingPlayers().remove(target.getPlayer());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lSystem &8" + MessageUtils.HEAVY_VERTICAL + " &7Build-Mode &c&lDisabled&7 for &e" + target.getPlayer().getName() + "."));
                target.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lSystem &8" + MessageUtils.HEAVY_VERTICAL + " &7Build-Mode &c&lDisabled&7 by STAFF."));
                target.getPlayer().setGameMode(GameMode.ADVENTURE);
                Items.formatInventory(target.getPlayer());
            } else {
                AsylumLobby.getInstance().getBuildingPlayers().add(sender);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lSystem &8" + MessageUtils.HEAVY_VERTICAL + " &7Build-Mode &a&lEnabled&7 for &e" + target.getPlayer().getName() + "."));
                target.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lSystem &8" + MessageUtils.HEAVY_VERTICAL + " &7Build-Mode &a&lEnabled&7 by STAFF."));
                target.getPlayer().setGameMode(GameMode.CREATIVE);
                target.getPlayer().getInventory().clear();
            }
        } else {
            if (!apSender.get().getPlayerData().getRank().isOwnerOrAdmin()) return;
            if (AsylumLobby.getInstance().getBuildingPlayers().contains(sender)) {
                AsylumLobby.getInstance().getBuildingPlayers().remove(sender);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lSystem &8" + MessageUtils.HEAVY_VERTICAL + " &7Build-Mode &c&lDisabled&7."));
                sender.setGameMode(GameMode.ADVENTURE);
                Items.formatInventory(sender);
            } else {
                AsylumLobby.getInstance().getBuildingPlayers().add(sender);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lSystem &8" + MessageUtils.HEAVY_VERTICAL + " &7Build-Mode &a&lEnabled&7."));
                sender.setGameMode(GameMode.CREATIVE);
                sender.getInventory().clear();
            }
        }
    }

}
