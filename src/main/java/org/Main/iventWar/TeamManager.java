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

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    // -------- Основные методы --------
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
            for (UUID member : team.getMembers()) {
                playerTeamMap.remove(member);
            }
            saveTeams();
            return true;
        }
        return false;
    }

    public Team getTeam(String name) {
        return teams.get(name);
    }

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

    // -------- Приглашения --------
    public void addInvitation(UUID player, String teamName) {
        pendingInvitations.put(player, teamName);
    }

    public String getInvitation(UUID player) {
        return pendingInvitations.get(player);
    }

    public void removeInvitation(UUID player) {
        pendingInvitations.remove(player);
    }

    // -------- Обновление Tab --------
    public void updateAllTeamTab(Team team) {
        for (UUID member : team.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                updatePlayerTab(player);
            }
        }
    }

    public void updatePlayerTab(Player player) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.setPlayerListName(player.getName());
            return;
        }

        String prefix = team.getPrefix(player.getUniqueId());
        // Используем getColoredNameWithBrackets() который применяет стиль
        String displayName = team.getColoredNameWithBrackets() + " " + player.getName() + " " + prefix;

        // Minecraft ограничивает длину имени в Tab до 64 символов
        if (displayName.length() > 64) {
            displayName = displayName.substring(0, 64);
        }
        player.setPlayerListName(displayName);
    }

    // -------- Сохранение / Загрузка --------
    public void saveTeams() {
        try (Writer writer = new FileWriter(dataFile)) {
            Map<String, TeamData> dataMap = new HashMap<>();
            for (Map.Entry<String, Team> entry : teams.entrySet()) {
                Team team = entry.getValue();
                TeamData data = new TeamData(
                        team.getLeader(),
                        new ArrayList<>(team.getMembers()),
                        team.getColor().name(),
                        team.getDescription(),
                        team.getTextFormat().name(), // Сохраняем стиль
                        new HashMap<>(team.getMembers().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        uuid -> uuid,
                                        uuid -> team.getPrefix(uuid)
                                ))
                        )
                );
                dataMap.put(entry.getKey(), data);
            }
            gson.toJson(dataMap, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save teams: " + e.getMessage());
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

                // Загружаем участников
                JsonArray membersArray = teamData.get("members").getAsJsonArray();
                for (JsonElement elem : membersArray) {
                    UUID member = UUID.fromString(elem.getAsString());
                    if (!member.equals(leader)) {
                        team.addMember(member);
                    }
                }

                // Загружаем цвет
                String colorName = teamData.get("color").getAsString();
                try {
                    team.setColor(ChatColor.valueOf(colorName));
                } catch (IllegalArgumentException e) {
                    team.setColor(ChatColor.WHITE);
                }

                // Загружаем стиль текста
                if (teamData.has("textFormat")) {
                    String formatName = teamData.get("textFormat").getAsString();
                    try {
                        team.setTextFormat(ChatColor.valueOf(formatName));
                    } catch (IllegalArgumentException e) {
                        team.setTextFormat(ChatColor.RESET);
                    }
                } else {
                    // Для старых файлов, где нет textFormat
                    team.setTextFormat(ChatColor.RESET);
                }

                // Загружаем описание
                team.setDescription(teamData.get("description").getAsString());

                // Загружаем позывные
                JsonObject prefixesObj = teamData.get("prefixes").getAsJsonObject();
                for (Map.Entry<String, JsonElement> prefEntry : prefixesObj.entrySet()) {
                    UUID player = UUID.fromString(prefEntry.getKey());
                    team.setPrefix(player, prefEntry.getValue().getAsString());
                }

                teams.put(teamName, team);
                for (UUID member : team.getMembers()) {
                    playerTeamMap.put(member, teamName);
                }
            }
            plugin.getLogger().info("Загружено " + teams.size() + " команд из файла");
        } catch (IOException | JsonSyntaxException e) {
            plugin.getLogger().severe("Could not load teams: " + e.getMessage());
        }
    }

    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    public Map<UUID, String> getPlayerTeamMap() {
        return new HashMap<>(playerTeamMap);
    }

    // -------- Вспомогательный класс для JSON --------
    private static class TeamData {
        private final UUID leader;
        private final List<UUID> members;
        private final String color;
        private final String description;
        private final String textFormat; // Новое поле
        private final Map<UUID, String> prefixes;

        public TeamData(UUID leader, List<UUID> members, String color,
                        String description, String textFormat, Map<UUID, String> prefixes) {
            this.leader = leader;
            this.members = members;
            this.color = color;
            this.description = description;
            this.textFormat = textFormat;
            this.prefixes = prefixes;
        }
    }
}