package org.Main.iventWar;

import org.bukkit.ChatColor;
import java.util.*;

public class Team {
    private final String name;
    private UUID leader;
    private final Set<UUID> members;
    private ChatColor color;
    private String description;
    private final Map<UUID, String> prefixes;

    public Team(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
        this.color = ChatColor.WHITE;
        this.description = "";
        this.prefixes = new HashMap<>();
        this.prefixes.put(leader, "");
    }

    public String getName() { return name; }
    public UUID getLeader() { return leader; }
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public ChatColor getColor() { return color; }
    public String getDescription() { return description; }
    public String getPrefix(UUID player) { return prefixes.getOrDefault(player, ""); }
    public String getColoredName() { return color + name; }
    public String getColoredNameWithBrackets() { return color + "[" + name + "]"; }

    public void setLeader(UUID leader) { if (members.contains(leader)) this.leader = leader; }
    public void setColor(ChatColor color) { this.color = color; }
    public void setDescription(String description) { this.description = description; }
    public void setPrefix(UUID player, String prefix) { if (members.contains(player)) prefixes.put(player, prefix); }

    public boolean addMember(UUID player) {
        if (!members.contains(player)) {
            members.add(player);
            prefixes.put(player, "");
            return true;
        }
        return false;
    }
    public boolean removeMember(UUID player) {
        if (members.contains(player) && !player.equals(leader)) {
            members.remove(player);
            prefixes.remove(player);
            return true;
        }
        return false;
    }
    public boolean isLeader(UUID player) { return leader.equals(player); }
    public boolean isMember(UUID player) { return members.contains(player); }
    public int getMemberCount() { return members.size(); }

    public String getDisplayName(UUID player) {
        String prefix = prefixes.getOrDefault(player, "");
        String prefixDisplay = prefix.isEmpty() ? "" : " (" + prefix + ")";
        return getColoredName() + " " + player.toString() + prefixDisplay;
    }

    public UUID transferLeadership() {
        if (members.size() <= 1) return null;
        for (UUID member : members) {
            if (!member.equals(leader)) {
                leader = member;
                return leader;
            }
        }
        return null;
    }
}
