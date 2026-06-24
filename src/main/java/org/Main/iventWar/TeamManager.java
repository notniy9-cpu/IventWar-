package org.Main.iventWar;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {
    private final IventWar plugin;
    private final Map<String, Team> teams;
    private final Map<UUID, String> playerTeamMap;
    private final Map<UUID, String> pendingInvitations;
    private final File dataFile;
    private final Gson gson;

    public TeamManager(IventWar plugin) {
        this.plugin = plugin;
        this.teams = new ConcurrentHashMap<>();
        this.playerTeamMap = new ConcurrentHashMap<>();
        this.pendingInvitations = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "teams.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
    }

    public Team createTeam(String name, UUID leader) {
        if (teams.containsKey(name)) return null;
        if (playerTeamMap.containsKey(leader)) return null;
        Team team = new Team(name, leader);
        teams.put(name, team);
        playerTeamMap.put(leader, name);
        saveTeams();
        return team;
    }

    public boolean deleteTeam(String name) {
        Team team = teams.remove(name);
        if (team != null) {
            for (UUID member : team.getMembers()) playerTeamMap.remove(member);
            saveTeams();
            return true;
        }
        return false;
    }

    public Team getTeam(String name) { return teams.get(name); }
    public Team getPlayerTeam(UUID player) {
        String teamName = playerTeamMap.get(player);
        return teamName != null ? teams.get(teamName) : null;
    }

    public boolean addPlayerToTeam(String teamName, UUID player) {
        Team team = teams.get(teamName);
        if (team == null) return false;
        if (playerTeamMap.containsKey(player)) return false;
        if (team.addMember(player)) {
            playerTeamMap.put(player, teamName);
            saveTeams();
            updateAllTeamTab(team);
            return true;
        }
        return false;
    }

    public boolean removePlayerFromTeam(UUID player) {
        String teamName = playerTeamMap.get(player);
        if (teamName == null) return false;
        Team team = teams.get(teamName);
        if (team == null) {
            playerTeamMap.remove(player);
            return false;
        }
        if (team.isLeader(player)) {
            UUID newLeader = team.transferLeadership();
            if (newLeader == null) {
                teams.remove(teamName);
                playerTeamMap.remove(player);
                saveTeams();
                return true;
            } else {
                playerTeamMap.remove(player);
                team.removeMember(player);
                saveTeams();
                Player newLeaderPlayer = Bukkit.getPlayer(newLeader);
                if (newLeaderPlayer != null) {
                    newLeaderPlayer.sendMessage(ChatColor.GOLD + "Теперь ты лидер команды '" +
                            team.getColoredName() + ChatColor.GOLD + "'!");
                }
                updateAllTeamTab(team);
                return true;
            }
        } else {
            if (team.removeMember(player)) {
                playerTeamMap.remove(player);
                saveTeams();
                updateAllTeamTab(team);
                return true;
            }
        }
        return false;
    }

    public boolean kickPlayerFromTeam(UUID leader, UUID target) {
        String teamName = playerTeamMap.get(leader);
        if (teamName == null) return false;
        Team team = teams.get(teamName);
        if (team == null) return false;
        if (!team.isLeader(leader)) return false;
        if (leader.equals(target)) return false;
        if (team.removeMember(target)) {
            playerTeamMap.remove(target);
            saveTeams();
            updateAllTeamTab(team);
            return true;
        }
        return false;
    }

    public boolean transferLeadership(UUID currentLeader, UUID newLeader) {
        String teamName = playerTeamMap.get(currentLeader);
        if (teamName == null) return false;
        Team team = teams.get(teamName);
        if (team == null) return false;
        if (!team.isLeader(currentLeader)) return false;
        if (!team.isMember(newLeader)) return false;
        if (currentLeader.equals(newLeader)) return false;
        team.setRole(currentLeader, "helper");
        team.setLeader(newLeader);
        team.setRole(newLeader, "leader");
        saveTeams();
        updateAllTeamTab(team);
        Player oldLeader = Bukkit.getPlayer(currentLeader);
        Player newLeaderP = Bukkit.getPlayer(newLeader);
        if (oldLeader != null) oldLeader.sendMessage(ChatColor.YELLOW + "Ты передал лидерство " + Bukkit.getOfflinePlayer(newLeader).getName());
        if (newLeaderP != null) newLeaderP.sendMessage(ChatColor.GOLD + "Теперь ты лидер команды '" + team.getColoredName() + ChatColor.GOLD + "'!");
        return true;
    }

    public boolean promoteToHelper(UUID leader, UUID target) {
        String teamName = playerTeamMap.get(leader);
        if (teamName == null) return false;
        Team team = teams.get(teamName);
        if (team == null) return false;
        if (!team.isLeader(leader)) return false;
        if (!team.isMember(target)) return false;
        if (team.isLeader(target)) return false;
        team.setRole(target, "helper");
        saveTeams();
        return true;
    }

    public boolean demoteToMember(UUID leader, UUID target) {
        String teamName = playerTeamMap.get(leader);
        if (teamName == null) return false;
        Team team = teams.get(teamName);
        if (team == null) return false;
        if (!team.isLeader(leader)) return false;
        if (!team.isMember(target)) return false;
        if (team.isLeader(target)) return false;
        team.setRole(target, "member");
        saveTeams();
        return true;
    }

    public void addInvitation(UUID player, String teamName) { pendingInvitations.put(player, teamName); }
    public String getInvitation(UUID player) { return pendingInvitations.get(player); }
    public void removeInvitation(UUID player) { pendingInvitations.remove(player); }

    public void updateAllTeamTab(Team team) {
        for (UUID member : team.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) updatePlayerDisplay(player);
        }
    }

    // ===== ОТОБРАЖЕНИЕ НАД ГОЛОВОЙ (ГАРАНТИРОВАННО) =====
    public void updatePlayerDisplay(Player player) {
        if (player == null) return;
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.setPlayerListName(player.getName());
            player.setDisplayName(player.getName());
            player.setCustomName(null);
            player.setCustomNameVisible(false);
            return;
        }

        String prefix = team.getPrefix(player.getUniqueId());
        String fullName = team.getColoredNameWithBrackets() + " " + player.getName() + " " + prefix;

        // Ограничиваем длину для таба
        String tabName = fullName.length() > 64 ? fullName.substring(0, 64) : fullName;

        player.setPlayerListName(tabName);
        player.setDisplayName(fullName);
        player.setCustomName(fullName);
        player.setCustomNameVisible(true);
    }

    public void saveTeams() {
        try (Writer writer = new FileWriter(dataFile)) {
            Map<String, TeamData> dataMap = new HashMap<>();
            for (Map.Entry<String, Team> entry : teams.entrySet()) {
                Team team = entry.getValue();
                Map<UUID, String> rawPrefixes = new HashMap<>();
                for (UUID member : team.getMembers()) {
                    rawPrefixes.put(member, team.getRawPrefix(member));
                }
                TeamData data = new TeamData(
                        team.getLeader(),
                        new ArrayList<>(team.getMembers()),
                        new HashMap<>(team.getMembers().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        uuid -> uuid,
                                        uuid -> team.getRole(uuid)
                                ))
                        ),
                        team.getColor().name(),
                        team.getDescription(),
                        rawPrefixes
                );
                dataMap.put(entry.getKey(), data);
            }
            gson.toJson(dataMap, writer);
        } catch (IOException e) {
            // Логируем только если это не ошибка "file not found"
            if (!(e instanceof FileNotFoundException)) {
                plugin.getLogger().warning("Не удалось сохранить команды: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadTeams() {
        if (!dataFile.exists()) return;
        try (Reader reader = new FileReader(dataFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String teamName = entry.getKey();
                JsonObject teamData = entry.getValue().getAsJsonObject();
                UUID leader = UUID.fromString(teamData.get("leader").getAsString());
                Team team = new Team(teamName, leader);

                JsonArray membersArray = teamData.get("members").getAsJsonArray();
                for (JsonElement elem : membersArray) {
                    UUID member = UUID.fromString(elem.getAsString());
                    if (!member.equals(leader)) team.addMember(member);
                }

                if (teamData.has("roles")) {
                    JsonObject rolesObj = teamData.get("roles").getAsJsonObject();
                    for (Map.Entry<String, JsonElement> roleEntry : rolesObj.entrySet()) {
                        UUID player = UUID.fromString(roleEntry.getKey());
                        String role = roleEntry.getValue().getAsString();
                        team.setRole(player, role);
                    }
                }

                String colorName = teamData.get("color").getAsString();
                try { team.setColor(ChatColor.valueOf(colorName)); } catch (Exception e) { team.setColor(ChatColor.WHITE); }

                team.setDescription(teamData.get("description").getAsString());

                JsonObject prefixesObj = teamData.get("prefixes").getAsJsonObject();
                for (Map.Entry<String, JsonElement> prefEntry : prefixesObj.entrySet()) {
                    UUID player = UUID.fromString(prefEntry.getKey());
                    String rawPrefix = prefEntry.getValue().getAsString();
                    team.setPrefix(player, rawPrefix);
                }

                teams.put(teamName, team);
                for (UUID member : team.getMembers()) playerTeamMap.put(member, teamName);
            }
            // Убираем лог, чтобы не спамить
            // plugin.getLogger().info("Загружено " + teams.size() + " команд");
        } catch (IOException | JsonSyntaxException e) {
            // Логируем только ошибки, не спамим
        }
    }

    public Collection<Team> getAllTeams() { return teams.values(); }

    private static class TeamData {
        private final UUID leader;
        private final List<UUID> members;
        private final Map<UUID, String> roles;
        private final String color;
        private final String description;
        private final Map<UUID, String> prefixes;
        public TeamData(UUID leader, List<UUID> members, Map<UUID, String> roles,
                        String color, String description, Map<UUID, String> prefixes) {
            this.leader = leader; this.members = members; this.roles = roles;
            this.color = color; this.description = description;
            this.prefixes = prefixes;
        }
    }
}