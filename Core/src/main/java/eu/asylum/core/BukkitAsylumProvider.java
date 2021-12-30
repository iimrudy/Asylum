package eu.asylum.core;

import eu.asylum.common.AsylumProvider;
import eu.asylum.common.configuration.ConfigurationContainer;
import lombok.NonNull;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BukkitAsylumProvider extends AsylumProvider<Player> implements Listener {


    public BukkitAsylumProvider(ConfigurationContainer<YamlConfiguration> configurationContainer) {
        super(configurationContainer);
    }

    @Override
    public List<Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    @Override
    public UUID getUUID(@NonNull Player player) {
        return player.getUniqueId();
    }

    @Override
    public String getUsername(@NonNull Player player) {
        return player.getName();
    }

    @Override
    public boolean isOnline(@NonNull Player player) {
        return player.isOnline();
    }

    @Override
    public void sendMessage(@NonNull Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void sendActionBar(@NonNull Player player, String message) {
        player.sendActionBar(MiniMessage.get().parse(message));
    }

    @Override
    public void sendTitle(@NonNull Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        var titleComponent = MiniMessage.get().parse(title);
        var subtitleComponent = MiniMessage.get().parse(subtitle);
        var time = Title.Times.of(Ticks.duration(fadeIn), Ticks.duration(stay), Ticks.duration(fadeOut));
        var t = Title.title(titleComponent, subtitleComponent, time);
        player.showTitle(t);
    }

    @EventHandler
    public void onPlayerJoinEvent(@NonNull PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(AsylumCore.getInstance(), () -> onJoin(event.getPlayer()));
        AsylumCore.getInstance().setupPrefix(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuitEvent(@NonNull PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(AsylumCore.getInstance(), () -> onQuit(event.getPlayer()));
    }
}
