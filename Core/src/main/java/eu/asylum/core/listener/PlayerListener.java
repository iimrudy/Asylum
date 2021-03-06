package eu.asylum.core.listener;

import eu.asylum.common.data.AsylumPlayer;
import eu.asylum.core.AsylumCore;
import eu.asylum.core.configuration.CoreConfiguration;
import eu.asylum.core.helpers.AsylumScoreBoard;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private static JaroWinklerDistance jaroWinklerDistance = null;
    private final Map<Player, String> lastMessage = new ConcurrentHashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        AsylumScoreBoard.createScore(event.getPlayer());
        lastMessage.put(event.getPlayer(), "");
        AsylumCore.getInstance().sendUpdate(false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(AsylumCore.getInstance(), () -> AsylumScoreBoard.removeScore(event.getPlayer()));
        lastMessage.remove(event.getPlayer());
        AsylumCore.getInstance().sendUpdate(false);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
        if (!CoreConfiguration.CHAT_ENABLED.getBoolean()) { // chat is disabled
            return;
        }

        var oap = AsylumCore.getInstance().getAsylumProvider().getAsylumPlayer(event.getPlayer());

        if (oap.isEmpty()) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9Chat> &7Cannot fetch your player data. Please try again later."));
            return;
        }

        final AsylumPlayer<Player> ap = oap.get();

        String msg = ((TextComponent) event.originalMessage()).content();
        String oldMsg = lastMessage.get(event.getPlayer());

        // Check similarity || Similarity is not checked for staff
        if (CoreConfiguration.CHAT_FILTER_REPETITION.getBoolean() && !ap.getRank().isStaff()) {
            if (jaroWinklerDistance == null) jaroWinklerDistance = new JaroWinklerDistance(); // lazy init

            // check if a string is similar to another with apache common string
            if (PlayerListener.jaroWinklerDistance.apply(oldMsg, msg) >= 0.9D) { // message is too similar
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9Chat> &7This message is too similar to the previous one."));
                return;
            }
        }

        // check for swears
        if (CoreConfiguration.CHAT_FILTER_SWEAR.getBoolean()) {
            if (!true) { // if is swearing
            }
        }

        if (CoreConfiguration.CHAT_GLOBAL_ENABLED.getBoolean() && msg.startsWith(CoreConfiguration.CHAT_GLOBAL_PREFIX.getString())) {
            msg = msg.substring(CoreConfiguration.CHAT_GLOBAL_PREFIX.getString().length()); // remove the prefix
            // send the prefix to redis and blah blah
            this.lastMessage.replace(event.getPlayer(), msg);


            return;
        }

        // Only Staff can use the chat
        if (CoreConfiguration.CHAT_FILTER_ONLY_STAFF.getBoolean() && !ap.getRank().isStaff()) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9Chat> &7At the moment, only the staff can use the chat."));
            return;
        }


        // pretty format the chat
        this.lastMessage.replace(event.getPlayer(), msg); // update last message, leave at the end.
        String prfx = ap.getRank().getPrefix();
        String after = "&e" + ap.getPlayerObject().getName() + "&7 " + msg;
        if (prfx.length() > 0) {
            after = " " + after;
        }
        Component newMessage = MiniMessage.get().parse(ap.getRank().getPrefix());
        newMessage = newMessage.append(LegacyComponentSerializer.legacyAmpersand().deserialize(after));
        Bukkit.broadcast(newMessage);
    }


}
