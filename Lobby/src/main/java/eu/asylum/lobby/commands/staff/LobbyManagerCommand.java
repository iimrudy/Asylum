package eu.asylum.lobby.commands.staff;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.PreCommand;
import co.aikar.commands.annotation.Subcommand;
import eu.asylum.common.utils.NekobinUploader;
import eu.asylum.core.AsylumCore;
import eu.asylum.lobby.AsylumLobby;
import eu.asylum.lobby.configuration.LobbyConfiguration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

@CommandAlias("lobbymanager|hubmanager|hm|lm")
public class LobbyManagerCommand extends BaseCommand {

    @PreCommand
    public boolean preCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            var ap = AsylumCore.getInstance().getAsylumProvider().getAsylumPlayer(player);
            return ap.isPresent() && ap.get().getRank().isOwnerOrAdmin();
        }
        return false;
    }

    @Subcommand("setspawn|sethub|setlobby")
    @CommandAlias("sethub|setlobby")
    public void setLobby(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aLobby setted to your current location."));
        LobbyConfiguration.HUB_SPAWN.set(player.getLocation());
        try {
            AsylumLobby.getInstance().getConfiguration().save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subcommand("reload")
    public void reloadConfiguration(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lArcade &8\u2503 &7Reloading configuration..."));
        try {
            AsylumLobby.getInstance().reload();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lArcade &8\u2503 &7Configuration reloaded &a&nsuccessfully&8."));
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lArcade &8\u2503 &7Something went wrong wile reloading the configuration. (Watch console for errors)"));
            NekobinUploader.upload(ExceptionUtils.getStackTrace(e)).thenAccept(result -> {
                Bukkit.getLogger().warning("Uploaded error to nekobin: " + result.isOk() + "   " + result.getError());
                if (result.getDocument() != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lArcade &8\u2503 &7Nekobin link: &a" + result.getDocument().asUrl()));
                }
            });
        }
    }

    @Subcommand("throwexcp")
    public void throwException(Player player) {
        throw new RuntimeException("SOME EXCEPTION " + System.currentTimeMillis());
    }

}
