package eu.asylum.lobby.commands.staff;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.PreCommand;
import co.aikar.commands.annotation.Subcommand;
import eu.asylum.core.AsylumCore;
import eu.asylum.lobby.AsylumLobby;
import eu.asylum.lobby.configuration.LobbyConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

@CommandAlias("lobbymanager|hubmanager|hm|lm")
public class LobbyManagerCommand extends BaseCommand {

    @PreCommand
    public boolean preCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            var ap = AsylumCore.getInstance().getAsylumProvider().getAsylumPlayer(player);
            return ap.isPresent() && ap.get().getRank().isOwnerOrAdmin();
        }
        return false;
    }

    @Subcommand("setspawn|sethub|setlobby")
    @CommandAlias("sethub|setlobby")
    public void setLobby(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aLobby setted to your current location."));
        LobbyConfiguration.HUB_SPAWN.set(AsylumLobby.getInstance().getConfiguration(), player.getLocation());
        try {
            AsylumLobby.getInstance().getConfiguration().save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
