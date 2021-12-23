package eu.asylum.core.helpers;

import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public class MessageUtils {

    private final static int CENTER_PX = 154;

    /**
     * send chat Message to a player
     *
     * @param message message to sand
     * @param type    alignment
     * @param players player list
     **/
    public static void sendChatMessage(@NonNull String message, @NonNull AlignmentType type, Player... players) {
        message = ChatColor.translateAlternateColorCodes('&', message); // fix colors
        if (type == AlignmentType.CENTERED) {
            message = centerMessage(message);
        } else if (type == AlignmentType.NORMAL) {
            //
        }
        // send message
        for (Player p : players) {
            p.sendMessage(message);
        }
    }

    /**
     * send chat Message to a player
     *
     * @param message message to sand
     * @param type    alignment
     * @param players player list
     **/
    public static void sendChatMessagePlayers(String message, AlignmentType type, Collection<Player> players) {
        message = ChatColor.translateAlternateColorCodes('&', message); // fix colors
        if (type == AlignmentType.CENTERED) {
            message = centerMessage(message);
        } else if (type == AlignmentType.NORMAL) {
            //
        }
        // send message
        for (Player p : players) {
            p.sendMessage(message);
        }
    }

    /**
     * Send actionbar to players
     *
     * @param message actionbar message
     * @param players to players
     **/
    public static void sendActionbar(@NonNull String message, Player... players) {
        message = ChatColor.translateAlternateColorCodes('&', message);
        for (Player p : players) {
            p.sendActionBar(message);
        }
    }

    /**
     * Send player actionbar percentage
     *
     * @param c1         percentage color
     * @param c2         inactive color
     * @param percentage actionbar percentage
     * @param players    player list
     **/
    public static void sendActionbarPercentagePrefix(ChatColor c1, ChatColor c2, int percentage, @NonNull Player... players) {
        sendActionbarPercentage('\u2588', "", "", c1, c2, percentage, players);
    }

    /**
     * Send player actionbar percentage
     *
     * @param prefix     prefix message
     * @param c1         percentage color
     * @param c2         inactive color
     * @param percentage actionbar percentage
     * @param players    player list
     **/
    public static void sendActionbarPercentagePrefix(String prefix, ChatColor c1, ChatColor c2, int percentage, @NonNull Player... players) {
        sendActionbarPercentage('\u2588', prefix, "", c1, c2, percentage, players);
    }

    /**
     * Send player actionbar percentage
     *
     * @param suffix     message suffix
     * @param c1         percentage color
     * @param c2         inactive color
     * @param percentage actionbar percentage
     * @param players    player list
     **/
    public static void sendActionbarPercentageSuffix(String suffix, ChatColor c1, ChatColor c2, int percentage, @NonNull Player... players) {
        sendActionbarPercentage('\u2588', "", suffix, c1, c2, percentage, players);
    }

    /**
     * Send player actionbar percentage appending a message specifying where
     *
     * @param character  bar character
     * @param prefix     message prefix
     * @param suffix     message suffix
     * @param c1         percentage color
     * @param c2         inactive color
     * @param percentage actionbar percentage
     * @param players    player list
     **/
    public static void sendActionbarPercentage(char character, String prefix, String suffix, @NonNull ChatColor c1, @NonNull ChatColor c2, int percentage, @NonNull Player... players) {
        String message = "";
        for (int i = 0; i < 10; i++) {
            message += character;
        }
        // 100 : messageLenght = percentage : x
        int amount = (message.length() * percentage) / 100;
        message = c1 + message.substring(0, amount) + c2 + message.substring(amount);

        if (prefix != null) {
            message = prefix + message;
        }
        if (suffix != null) {
            message += suffix;
        }
        sendActionbar(message, players);
    }

    /**
     * Send player actionbar percentage appending a message specifying where
     *
     * @param prefix     message prefix
     * @param suffix     message suffix
     * @param c1         percentage color
     * @param c2         inactive color
     * @param percentage actionbar percentage
     * @param players    player list
     **/
    public static void sendActionbarPercentage(String prefix, String suffix, @NonNull ChatColor c1, @NonNull ChatColor c2, int percentage, @NonNull Player... players) {
        sendActionbarPercentage('\u2588', prefix, suffix, c1, c2, percentage, players);
    }

    public static String centerMessage(@NonNull String message) {

        if (message == null || message.equals("")) return "";
        message = ChatColor.translateAlternateColorCodes('&', message);

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : message.toCharArray()) {
            if (c == '\u00A7') {
                previousCode = true;
                continue;
            } else if (previousCode == true) {
                previousCode = false;
                if (c == 'l' || c == 'L') {
                    isBold = true;
                    continue;
                } else isBold = false;
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb + message;
    }

    public enum AlignmentType {
        NORMAL,
        CENTERED
    }


}
