package eu.asylum.core.helpers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class AsylumScoreBoard {

    private static final Map<UUID, AsylumScoreBoard> players = new HashMap<>();
    private final Scoreboard scoreboard;
    private final Objective sidebar;

    private AsylumScoreBoard(Player player) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        sidebar = scoreboard.registerNewObjective("sidebar", "dummy");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        // Create Teams
        for (int i = 1; i <= 15; i++) {
            var team = scoreboard.registerNewTeam("SLOT_" + i);
            team.addEntry(genEntry(i));
        }
        player.setScoreboard(scoreboard);
        synchronized (AsylumScoreBoard.players) {
            players.put(player.getUniqueId(), this);
        }
    }

    public static boolean hasScore(Player player) {
        boolean contains;
        synchronized (AsylumScoreBoard.players) {
            contains = players.containsKey(player.getUniqueId());
        }
        return contains;
    }

    public static AsylumScoreBoard createScore(Player player) {
        return new AsylumScoreBoard(player);
    }

    public static AsylumScoreBoard getByPlayer(Player player) {
        AsylumScoreBoard cached;
        synchronized (AsylumScoreBoard.players) {
            cached = players.get(player.getUniqueId());
        }
        return cached;
    }

    public static void removeScore(Player player) {
        players.remove(player.getUniqueId());
    }

    public static void flush() {
        players.clear();
    }

    public void setTitle(String title) {
        title = ChatColor.translateAlternateColorCodes('&', title);
        sidebar.setDisplayName(title.length() > 32 ? title.substring(0, 32) : title);
    }

    public void setSlot(int slot, String text) {
        this.setSlot(slot, new ScoreboardRow(text));
    }

    public void setSlot(int slot, ScoreboardRow row) {
        Team team = scoreboard.getTeam("SLOT_" + slot);
        String entry = genEntry(slot);
        if (!scoreboard.getEntries().contains(entry)) {
            sidebar.getScore(entry).setScore(slot);
        }
        team.setPrefix(row.getPrefix());
        team.setSuffix(row.getSuffix());
    }

    public void removeSlot(int slot) {
        String entry = genEntry(slot);
        if (scoreboard.getEntries().contains(entry)) {
            scoreboard.resetScores(entry);
        }
    }

    public void setSlotsFromList(List<String> list) {
        ArrayList<ScoreboardRow> rows = new ArrayList<>();
        for (int i = 0; i < list.size() && i < 15; i++) {
            rows.add(new ScoreboardRow(list.get(i)));
        }
        this.setSlotsFromListRows(rows);
    }

    public void setSlotsFromListRows(List<ScoreboardRow> rows) {
        if (rows.size() > 15) {
            rows = rows.subList(0, 15);
        }

        int slot = rows.size();

        if (slot < 15) {
            for (int i = (slot + 1); i <= 15; i++) {
                removeSlot(i);
            }
        }

        for (ScoreboardRow row : rows) {
            setSlot(slot, row);
            slot--;
        }
    }

    private String genEntry(int slot) {
        return ChatColor.values()[slot].toString();
    }

    /**
     * @author serega6531
     * @lastEditor iim_rudy
     * @link https://gist.github.com/serega6531/4acd23ac188c8c568287
     **/
    private class ScoreboardRow {

        private String prefix, suffix;

        public ScoreboardRow(String row) {
            row = ChatColor.translateAlternateColorCodes('&', row);
            if (row.length() <= 16) {
                prefix = row;
                suffix = "";
            } else {     //up to 16+16, color pair is in single part
                int cut = findCutPoint(row);
                prefix = row.substring(0, cut);
                suffix = continueColors(prefix) + row.substring(cut, row.length());

                if (suffix.length() > 16) {
                    suffix = suffix.substring(0, 16);
                }
            }
        }

        private int findCutPoint(String s) {
            for (int i = 16; i > 0; i--) {
                if (s.charAt(i - 1) == ChatColor.COLOR_CHAR && ChatColor.getByChar(s.charAt(i)) != null)
                    continue;
                return i;
            }
            return 16;
        }

        private String continueColors(String prefix) {
            ChatColor activeColor = null;
            Set<ChatColor> formats = new HashSet<>();

            for (int i = 0; i < prefix.length() - 1; i++) {
                char c1 = prefix.charAt(i);
                char c2 = prefix.charAt(i + 1);

                ChatColor color = ChatColor.getByChar(c2);
                if (c1 == ChatColor.COLOR_CHAR && color != null) {
                    if (color == ChatColor.RESET) {
                        activeColor = null;
                        formats.clear();
                    } else if (color.isColor()) {
                        activeColor = color;
                    } else {
                        formats.add(color);
                    }
                }
            }

            StringBuffer sb = new StringBuffer();

            if (activeColor != null)
                sb.append(activeColor);
            formats.forEach(format -> sb.append(format.toString()));

            return sb.toString();
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        @Override
        public String toString() {
            return prefix + suffix;
        }
    }

}
