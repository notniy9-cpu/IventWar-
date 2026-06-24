package org.Main.iventWar;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class EventManager {
    private final IventWar plugin;
    private boolean isEventActive;
    private int eventDuration;
    private int remainingSeconds;
    private String selectedTeam;
    private String weather;
    private String timeOfDay;
    private BukkitTask countdownTask;
    private BukkitTask eventTimerTask;

    public EventManager(IventWar plugin) {
        this.plugin = plugin;
        this.isEventActive = false;
        this.eventDuration = 0;
        this.selectedTeam = "all";
        this.weather = "clear";
        this.timeOfDay = "day";
    }

    public boolean isEventActive() { return isEventActive; }
    public void setEventDuration(int minutes) { this.eventDuration = minutes; }
    public int getEventDuration() { return eventDuration; }
    public void setSelectedTeam(String team) { this.selectedTeam = team; }
    public String getSelectedTeam() { return selectedTeam; }
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

        isEventActive = true;

        // Обратный отсчёт 5-4-3-2-1
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
                    startEventTimer();
                    if (countdownTask != null) countdownTask.cancel();
                }
            }
        }, 0L, 20L);
    }

    private void applyEventSettings() {
        for (World world : Bukkit.getWorlds()) {
            switch (weather.toLowerCase()) {
                case "clear":
                    world.setStorm(false);
                    world.setThundering(false);
                    break;
                case "rain":
                    world.setStorm(true);
                    world.setThundering(false);
                    break;
                case "thunder":
                    world.setStorm(true);
                    world.setThundering(true);
                    break;
                default:
                    world.setStorm(false);
                    world.setThundering(false);
                    break;
            }
            switch (timeOfDay.toLowerCase()) {
                case "day":
                    world.setTime(1000);
                    break;
                case "night":
                    world.setTime(13000);
                    break;
                case "sunset":
                    world.setTime(12000);
                    break;
                case "sunrise":
                    world.setTime(0);
                    break;
                case "keep":
                    break;
                default:
                    break;
            }
        }
    }

    private void startEventTimer() {
        remainingSeconds = eventDuration * 60;

        if (eventTimerTask != null && !eventTimerTask.isCancelled()) {
            eventTimerTask.cancel();
        }

        eventTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    endEvent();
                    if (eventTimerTask != null) eventTimerTask.cancel();
                    return;
                }

                if (remainingSeconds % 60 == 0) {
                    int minutesLeft = remainingSeconds / 60;
                    if (minutesLeft <= 5 && minutesLeft > 0) {
                        Bukkit.broadcastMessage("§eОсталось §6" + minutesLeft + " §eминут до окончания ивента!");
                    }
                }
                remainingSeconds--;
            }
        }, 0L, 20L);
    }

    private void endEvent() {
        isEventActive = false;
        Bukkit.broadcastMessage("§a§lИвент завершён! §7Спасибо за участие!");

        for (World world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
        }

        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }
        if (eventTimerTask != null && !eventTimerTask.isCancelled()) {
            eventTimerTask.cancel();
        }
    }

    public void cancelEvent() {
        if (isEventActive) {
            isEventActive = false;
            Bukkit.broadcastMessage("§cИвент был отменён администратором!");
            if (countdownTask != null && !countdownTask.isCancelled()) {
                countdownTask.cancel();
            }
            if (eventTimerTask != null && !eventTimerTask.isCancelled()) {
                eventTimerTask.cancel();
            }
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(false);
                world.setThundering(false);
            }
        }
    }
}