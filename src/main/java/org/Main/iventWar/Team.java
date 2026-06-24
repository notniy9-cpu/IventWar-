package org.Main.iventWar;

import org.bukkit.ChatColor;
import java.util.*;

public class Team {
    private final String name;
    private UUID leader;
    private final Set<UUID> members;
    private final Map<UUID, String> roles;
    private ChatColor color;
    private String description;
    private final Map<UUID, String> prefixes;

    public Team(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
        this.roles = new HashMap<>();
        this.roles.put(leader, "leader");
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
    public String getRole(UUID player) { return roles.getOrDefault(player, "member"); }

    public String getPrefix(UUID player) {
        String prefix = prefixes.getOrDefault(player, "");
        return prefix.isEmpty() ? "" : ChatColor.GRAY + "(" + prefix + ")";
    }

    public String getColoredName() { return color + name; }
    public String getColoredNameWithBrackets() { return color + "[" + name + "]"; }

    public void setLeader(UUID leader) {
        if (members.contains(leader)) {
            this.leader = leader;
            roles.put(leader, "leader");
        }
    }

    public void setColor(ChatColor color) { this.color = color; }
    public void setDescription(String description) { this.description = description; }
    public void setPrefix(UUID player, String prefix) {
        if (members.contains(player)) prefixes.put(player, prefix);
    }

    public void setRole(UUID player, String role) {
        if (members.contains(player)) roles.put(player, role);
    }

    public boolean isLeader(UUID player) { return leader.equals(player); }
    public boolean isHelper(UUID player) { return roles.getOrDefault(player, "member").equals("helper"); }
    public boolean isMember(UUID player) { return members.contains(player); }

    public boolean addMember(UUID player) {
        if (!members.contains(player)) {
            members.add(player);
            roles.put(player, "member");
            prefixes.put(player, "");
            return true;
        }
        return false;
    }

    public boolean removeMember(UUID player) {
        if (members.contains(player) && !player.equals(leader)) {
            members.remove(player);
            roles.remove(player);
            prefixes.remove(player);
            return true;
        }
        return false;
    }

    public int getMemberCount() { return members.size(); }

    public UUID transferLeadership() {
        if (members.size() <= 1) return null;
        for (UUID member : members) {
            if (!member.equals(leader)) {
                roles.put(leader, "helper");
                leader = member;
                roles.put(leader, "leader");
                return leader;
            }
        }
        return null;
    }
}