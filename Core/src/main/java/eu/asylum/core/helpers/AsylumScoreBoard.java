package eu.asylum.core.helpers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public static AsylumScoreBoard removeScore(Player player) {
        AsylumScoreBoard cached;
        synchronized (AsylumScoreBoard.players) {
            cached = players.remove(player.getUniqueId());
        }
        return cached;
    }

    public static void flush() {
        players.clear();
    }

    public void setTitle(String title) {
        title = ChatColor.translateAlternateColorCodes('&', title);
        sidebar.setDisplayName(title.length() > 32 ? title.substring(0, 32) : title);
    }

    public void setSlot(int slot, String text) {
        Team team = scoreboard.getTeam("SLOT_" + slot);
        String entry = genEntry(slot);
        if (!scoreboard.getEntries().contains(entry)) {
            sidebar.getScore(entry).setScore(slot);
        }
        text = ChatColor.translateAlternateColorCodes('&', text);
        String pre = getFirstSplit(text);
        String suf = getFirstSplit(ChatColor.getLastColors(pre) + getSecondSplit(text));
        team.setPrefix(pre);
        team.setSuffix(suf);
    }

    public void removeSlot(int slot) {
        String entry = genEntry(slot);
        if (scoreboard.getEntries().contains(entry)) {
            scoreboard.resetScores(entry);
        }
    }

    public void setSlotsFromList(List<String> list) {
        while (list.size() > 15) {
            list.remove(list.size() - 1);
        }

        int slot = list.size();

        if (slot < 15) {
            for (int i = (slot + 1); i <= 15; i++) {
                removeSlot(i);
            }
        }

        for (String line : list) {
            setSlot(slot, line);
            slot--;
        }
    }

    private String genEntry(int slot) {
        return ChatColor.values()[slot].toString();
    }

    private String getFirstSplit(String s) {
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    private String getSecondSplit(String s) {
        if (s.length() > 32) {
            s = s.substring(0, 32);
        }
        return s.length() > 16 ? s.substring(16) : "";
    }

}
