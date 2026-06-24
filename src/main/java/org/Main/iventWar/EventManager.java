package org.Main.iventWar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {
    private final IventWar plugin;
    private boolean isEventActive;
    private int eventDuration;
    private int remainingSeconds;
    private List<String> selectedTeams;
    private Map<String, Location> teamSpawnLocations;
    private Location endSpawnLocation;
    private String weather;
    private String timeOfDay;
    private BukkitTask countdownTask;
    private BukkitTask eventTimerTask;
    private BukkitTask actionBarTask;

    public EventManager(IventWar plugin) {
        this.plugin = plugin;
        this.isEventActive = false;
        this.eventDuration = 0;
        this.selectedTeams = new ArrayList<>();
        this.teamSpawnLocations = new HashMap<>();
        this.endSpawnLocation = null;
        this.weather = "clear";
        this.timeOfDay = "day";
    }

    public boolean isEventActive() { return isEventActive; }
    public void setEventDuration(int minutes) { this.eventDuration = minutes; }
    public int getEventDuration() { return eventDuration; }
    public List<String> getSelectedTeams() { return selectedTeams; }
    public void setSelectedTeams(List<String> teams) { selectedTeams = teams; }
    public void addTeam(String team) { if (!selectedTeams.contains(team)) selectedTeams.add(team); }
    public void removeTeam(String team) { selectedTeams.remove(team); }
    public void clearTeams() { selectedTeams.clear(); }
    public Map<String, Location> getTeamSpawnLocations() { return teamSpawnLocations; }
    public void setTeamSpawnLocation(String team, Location loc) { teamSpawnLocations.put(team, loc); }
    public void removeTeamSpawnLocation(String team) { teamSpawnLocations.remove(team); }
    public void clearAllSpawnLocations() { teamSpawnLocations.clear(); }
    public Location getEndSpawnLocation() { return endSpawnLocation; }
    public void setEndSpawnLocation(Location loc) { this.endSpawnLocation = loc; }
    public void clearEndSpawnLocation() { this.endSpawnLocation = null; }
    public void setWeather(String weather) { this.weather = weather; }
    public String getWeather() { return weather; }
    public void setTimeOfDay(String time) { this.timeOfDay = time; }
    public String getTimeOfDay() { return timeOfDay; }

    public void startEvent() {
        if (isEventActive) {
            Bukkit.broadcastMessage("§cИвент уже запущен!");
            return;
        }
        if (eventDuration <= 0) {
            Bukkit.broadcastMessage("§cУстановите длительность ивента!");
            return;
        }
        if (selectedTeams.isEmpty()) {
            Bukkit.broadcastMessage("§cВыберите хотя бы одну команду для участия!");
            return;
        }

        isEventActive = true;

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int count = 5;
            @Override
            public void run() {
                if (count > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§6§l" + count, "§eДо начала ивента...", 10, 20, 10);
                    }
                    count--;
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§a§lНАЧАЛИ!", "§7Ивент стартовал!", 10, 60, 10);
                    }
                    applyEventSettings();
                    teleportTeams();
                    startEventTimer();
                    startActionBarTimer();
                    if (countdownTask != null) countdownTask.cancel();
                }
            }
        }, 0L, 20L);
    }

    private void teleportTeams() {
        for (Map.Entry<String, Location> entry : teamSpawnLocations.entrySet()) {
            String teamName = entry.getKey();
            Location loc = entry.getValue();
            Team team = plugin.getTeamManager().getTeam(teamName);
            if (team != null) {
                for (UUID member : team.getMembers()) {
                    Player p = Bukkit.getPlayer(member);
                    if (p != null && p.isOnline()) p.teleport(loc);
                }
            }
        }
        for (String teamName : selectedTeams) {
            if (!teamSpawnLocations.containsKey(teamName)) {
                Team team = plugin.getTeamManager().getTeam(teamName);
                if (team != null) {
                    World world = Bukkit.getWorlds().get(0);
                    Location defaultSpawn = world.getSpawnLocation();
                    for (UUID member : team.getMembers()) {
                        Player p = Bukkit.getPlayer(member);
                        if (p != null && p.isOnline()) p.teleport(defaultSpawn);
                    }
                }
            }
        }
    }

    private void applyEventSettings() {
        for (World world : Bukkit.getWorlds()) {
            switch (weather.toLowerCase()) {
                case "clear": world.setStorm(false); world.setThundering(false); break;
                case "rain": world.setStorm(true); world.setThundering(false); break;
                case "thunder": world.setStorm(true); world.setThundering(true); break;
                default: world.setStorm(false); world.setThundering(false); break;
            }
            switch (timeOfDay.toLowerCase()) {
                case "day": world.setTime(1000); break;
                case "night": world.setTime(13000); break;
                case "sunset": world.setTime(12000); break;
                case "sunrise": world.setTime(0); break;
                case "keep": break;
                default: break;
            }
        }
    }

    private void startEventTimer() {
        remainingSeconds = eventDuration * 60;
        if (eventTimerTask != null && !eventTimerTask.isCancelled()) eventTimerTask.cancel();
        eventTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    endEvent();
                    if (eventTimerTask != null) eventTimerTask.cancel();
                    return;
                }
                remainingSeconds--;
            }
        }, 0L, 20L);
    }

    private void startActionBarTimer() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) actionBarTask.cancel();
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!isEventActive) { actionBarTask.cancel(); return; }
                int totalSeconds = remainingSeconds;
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                String timeStr = String.format("§6§l⏳ %02d:%02d", minutes, seconds);
                for (Player p : Bukkit.getOnlinePlayers()) p.sendActionBar(timeStr);
            }
        }, 0L, 20L);
    }

    public void forceCloseEvent(String reason) {
        if (!isEventActive) { Bukkit.broadcastMessage("§cИвент не активен!"); return; }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§c§lИВЕНТ ЗАВЕРШЁН!", "§eПричина: §f" + reason, 10, 100, 10);
            p.sendMessage("§cИвент завершён досрочно! §eПричина: §f" + reason);
            p.getInventory().clear();
            if (endSpawnLocation != null) p.teleport(endSpawnLocation);
        }
        resetAll();
    }

    private void endEvent() {
        isEventActive = false;
        Bukkit.broadcastMessage("§a§lИвент завершён! §7Спасибо за участие!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            if (endSpawnLocation != null) p.teleport(endSpawnLocation);
        }
        for (World world : Bukkit.getWorlds()) { world.setStorm(false); world.setThundering(false); }
        if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();
        if (eventTimerTask != null && !eventTimerTask.isCancelled()) eventTimerTask.cancel();
        if (actionBarTask != null && !actionBarTask.isCancelled()) actionBarTask.cancel();
    }

    private void resetAll() {
        if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();
        if (eventTimerTask != null && !eventTimerTask.isCancelled()) eventTimerTask.cancel();
        if (actionBarTask != null && !actionBarTask.isCancelled()) actionBarTask.cancel();
        isEventActive = false;
        eventDuration = 0;
        remainingSeconds = 0;
        selectedTeams.clear();
        teamSpawnLocations.clear();
        weather = "clear";
        timeOfDay = "day";
        for (World world : Bukkit.getWorlds()) { world.setStorm(false); world.setThundering(false); }
        Bukkit.broadcastMessage("§aВсе настройки ивента сброшены.");
    }

    public void cancelEvent() {
        if (isEventActive) {
            isEventActive = false;
            Bukkit.broadcastMessage("§cИвент был отменён администратором!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().clear();
                if (endSpawnLocation != null) p.teleport(endSpawnLocation);
            }
            if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();
            if (eventTimerTask != null && !eventTimerTask.isCancelled()) eventTimerTask.cancel();
            if (actionBarTask != null && !actionBarTask.isCancelled()) actionBarTask.cancel();
            for (World world : Bukkit.getWorlds()) { world.setStorm(false); world.setThundering(false); }
        }
    }
}