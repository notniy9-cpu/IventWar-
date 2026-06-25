package org.Main.iventWar;

import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.*;
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
    private BukkitTask cycleTask;

    private final Map<EventType, EventSettings> eventSettings;
    private final Map<EventType, BukkitTask> activeTasks;
    private final Map<EventType, Location> eventLocations;
    private final Map<EventType, Integer> eventTimers;

    private boolean cycleEnabled = false;
    private int cycleInterval = 5;
    private int lastNotifiedMinute = 0;

    private final File dataFile;
    private final Gson gson;

    public EventManager(IventWar plugin) {
        this.plugin = plugin;
        this.isEventActive = false;
        this.eventDuration = 0;
        this.selectedTeams = new ArrayList<>();
        this.teamSpawnLocations = new HashMap<>();
        this.endSpawnLocation = null;
        this.weather = "clear";
        this.timeOfDay = "day";

        this.eventSettings = new HashMap<>();
        this.activeTasks = new HashMap<>();
        this.eventLocations = new HashMap<>();
        this.eventTimers = new HashMap<>();

        this.dataFile = new File(plugin.getDataFolder(), "events.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        for (EventType type : EventType.values()) {
            EventSettings settings = new EventSettings(type);
            settings.setEnabled(true);
            if (type == EventType.AIRDROP) {
                settings.getLootItems().add(new EventSettings.LootItem(new ItemStack(Material.DIAMOND, 5), 0.5));
                settings.getLootItems().add(new EventSettings.LootItem(new ItemStack(Material.EMERALD, 10), 0.7));
            }
            eventSettings.put(type, settings);
        }

        loadEvents();
    }

    // ----- Геттеры -----
    public boolean isCycleEnabled() { return cycleEnabled; }
    public int getCycleInterval() { return cycleInterval; }
    public void setCycleEnabled(boolean enabled) { this.cycleEnabled = enabled; }
    public void setCycleInterval(int minutes) { this.cycleInterval = Math.max(1, minutes); }

    public boolean isEventActive() { return isEventActive; }
    public void setEventDuration(int minutes) { this.eventDuration = minutes; }
    public int getEventDuration() { return eventDuration; }
    public List<String> getSelectedTeams() { return selectedTeams; }
    public void setSelectedTeams(List<String> teams) { this.selectedTeams = teams; }
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

    // ----- Запуск основного ивента -----
    public void startMainEvent() {
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
        lastNotifiedMinute = eventDuration;

        countdownTask = new BukkitRunnable() {
            int count = 5;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                if (count > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
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
                    startAutoEvents();
                    if (cycleEnabled) startCycle();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
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

    private void startEventTimer() {
        remainingSeconds = eventDuration * 60;
        if (eventTimerTask != null && !eventTimerTask.isCancelled()) eventTimerTask.cancel();
        eventTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                if (remainingSeconds <= 0) {
                    endMainEvent();
                    cancel();
                    return;
                }
                int minutesLeft = remainingSeconds / 60;
                if (minutesLeft > 0 && minutesLeft % 5 == 0 && minutesLeft != lastNotifiedMinute) {
                    lastNotifiedMinute = minutesLeft;
                    Bukkit.broadcastMessage("§6§l⏳ Осталось §e" + minutesLeft + " §6минут до окончания ивента!");
                }
                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startAutoEvents() {
        EventSettings airdropSettings = eventSettings.get(EventType.AIRDROP);
        if (airdropSettings.isEnabled()) {
            int delay = airdropSettings.getDelay();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                startEvent(EventType.AIRDROP);
            }, delay * 20L);
        }

        EventSettings bombSettings = eventSettings.get(EventType.BOMBARDMENT);
        if (bombSettings.isEnabled()) {
            int delay = bombSettings.getDelay();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                startEvent(EventType.BOMBARDMENT);
            }, delay * 20L);
        }

        EventSettings nuclearSettings = eventSettings.get(EventType.NUCLEAR);
        if (nuclearSettings.isEnabled()) {
            int delay = nuclearSettings.getDelay();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                startEvent(EventType.NUCLEAR);
            }, delay * 20L);
        }
    }

    private void startCycle() {
        if (cycleTask != null && !cycleTask.isCancelled()) cycleTask.cancel();
        cycleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled() || !isEventActive || !cycleEnabled) {
                    cancel();
                    return;
                }
                List<EventType> available = new ArrayList<>();
                for (EventType type : EventType.values()) {
                    if (eventSettings.get(type).isEnabled() && !isEventActive(type)) {
                        available.add(type);
                    }
                }
                if (!available.isEmpty()) {
                    EventType chosen = available.get(new Random().nextInt(available.size()));
                    startEvent(chosen);
                    Bukkit.broadcastMessage("§eЦикл событий: запущено §6" + chosen.name());
                }
            }
        }.runTaskTimer(plugin, cycleInterval * 60 * 20L, cycleInterval * 60 * 20L);
    }

    private void stopCycle() {
        if (cycleTask != null && !cycleTask.isCancelled()) {
            cycleTask.cancel();
            cycleTask = null;
        }
    }

    public void forceCloseEvent(String reason) {
        if (!isEventActive) {
            Bukkit.broadcastMessage("§cИвент не активен!");
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§c§lИВЕНТ ЗАВЕРШЁН!", "§eПричина: §f" + reason, 10, 100, 10);
            p.sendMessage("§cИвент завершён досрочно! §eПричина: §f" + reason);
            p.getInventory().clear();
            if (endSpawnLocation != null) p.teleport(endSpawnLocation);
        }
        resetAll();
    }

    private void endMainEvent() {
        isEventActive = false;
        Bukkit.broadcastMessage("§a§lИвент завершён! §7Спасибо за участие!");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            if (endSpawnLocation != null) p.teleport(endSpawnLocation);
        }
        for (World world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
        }
        if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();
        if (eventTimerTask != null && !eventTimerTask.isCancelled()) eventTimerTask.cancel();
        stopCycle();
    }

    private void resetAll() {
        if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();
        if (eventTimerTask != null && !eventTimerTask.isCancelled()) eventTimerTask.cancel();
        stopCycle();

        isEventActive = false;
        eventDuration = 0;
        remainingSeconds = 0;
        selectedTeams.clear();
        teamSpawnLocations.clear();
        weather = "clear";
        timeOfDay = "day";
        for (World world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
        }
        Bukkit.broadcastMessage("§aВсе настройки ивента сброшены.");
    }

    // ----- Методы для событий -----
    public EventSettings getSettings(EventType type) {
        return eventSettings.get(type);
    }

    public boolean isEventActive(EventType type) {
        return activeTasks.containsKey(type) && !activeTasks.get(type).isCancelled();
    }

    public void startEvent(EventType type) {
        if (isEventActive(type)) return;
        EventSettings config = eventSettings.get(type);
        if (!config.isEnabled()) return;
        switch (type) {
            case AIRDROP: startAirdrop(); break;
            case BOMBARDMENT: startBombardment(); break;
            case NUCLEAR: startNuclear(); break;
        }
    }

    public void stopEvent(EventType type) {
        if (activeTasks.containsKey(type)) {
            activeTasks.get(type).cancel();
            activeTasks.remove(type);
        }
        Bukkit.broadcastMessage("§cСобытие " + type.name() + " остановлено!");
    }

    public Location getEventLocation(EventType type) {
        return eventLocations.get(type);
    }

    // ===== Общие координаты =====
    public void setCommonCoordinates(Location loc) {
        for (EventType type : EventType.values()) {
            eventSettings.get(type).setCenter(loc);
        }
        Bukkit.broadcastMessage("§aОбщие координаты установлены для всех событий: §6" +
                loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        saveEvents();
    }

    public void clearCommonCoordinates() {
        for (EventType type : EventType.values()) {
            eventSettings.get(type).setCenter(null);
        }
        Bukkit.broadcastMessage("§aКоординаты для всех событий сброшены.");
        saveEvents();
    }

    // ========================================================================
    // АЭРОДРОП
    // ========================================================================
    private void startAirdrop() {
        EventSettings config = eventSettings.get(EventType.AIRDROP);
        Location center = config.getCenter();
        int radius = config.getRadius();
        if (center == null) {
            center = Bukkit.getWorlds().get(0).getSpawnLocation();
            config.setCenter(center);
        }

        Location dropLocation = getRandomLocation(center, radius);
        Location groundLocation = dropLocation.clone();
        groundLocation.setY(groundLocation.getWorld().getHighestBlockYAt(groundLocation) + 1);

        final int duration = config.getDuration();

        Bukkit.broadcastMessage("§6§lАЭРОДРОП! §eЧерез 20 секунд по координатам: §6" +
                groundLocation.getBlockX() + " " + groundLocation.getBlockY() + " " + groundLocation.getBlockZ());

        BukkitTask preGlowTask = new BukkitRunnable() {
            int countdown = 20;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                if (countdown <= 0) {
                    spawnFallingChest(groundLocation);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        createAirdropChest(groundLocation);
                        startChestTimer(groundLocation, duration);
                    }, 40L);
                    eventTimers.remove(EventType.AIRDROP);
                    cancel();
                    return;
                }
                eventTimers.put(EventType.AIRDROP, -countdown);
                for (int i = 0; i < 360; i += 15) {
                    double angle = Math.toRadians(i);
                    double x = groundLocation.getX() + 3 * Math.cos(angle);
                    double z = groundLocation.getZ() + 3 * Math.sin(angle);
                    Location particleLoc = new Location(groundLocation.getWorld(), x, groundLocation.getY(), z);
                    groundLocation.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        activeTasks.put(EventType.AIRDROP, preGlowTask);
        eventLocations.put(EventType.AIRDROP, groundLocation);
    }

    private void spawnFallingChest(Location location) {
        World world = location.getWorld();
        Location spawnLoc = location.clone().add(0, 20, 0);
        FallingBlock chest = world.spawnFallingBlock(spawnLoc, Material.CHEST.createBlockData());
        chest.setDropItem(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (chest.isDead() || chest.isOnGround()) {
                    world.playSound(chest.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    world.spawnParticle(Particle.EXPLOSION_NORMAL, chest.getLocation(), 30);
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.FLAME, chest.getLocation().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ===== ИСПРАВЛЕНО: создание сундука с лутом =====
    private void createAirdropChest(Location location) {
        try {
            location.getBlock().setType(Material.CHEST);
            Inventory chest = (Inventory) location.getBlock().getState();
            // Получаем актуальный список лута из настроек
            List<EventSettings.LootItem> lootItems = eventSettings.get(EventType.AIRDROP).getLootItems();
            if (lootItems != null && !lootItems.isEmpty()) {
                int added = 0;
                for (EventSettings.LootItem loot : lootItems) {
                    if (loot.getItem() != null && Math.random() < loot.getChance()) {
                        chest.addItem(loot.getItem().clone());
                        added++;
                    }
                }
                Bukkit.broadcastMessage("§a§lАЭРОДРОП ПРИБЫЛ! §eДобавлено " + added + " предметов.");
            } else {
                Bukkit.broadcastMessage("§a§lАЭРОДРОП ПРИБЫЛ! §eСундук пуст (нет лута).");
            }
            Bukkit.broadcastMessage("§eПо координатам: §6" +
                    location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
        } catch (Exception e) {
            // ignore
        }
    }

    private void startChestTimer(Location location, int duration) {
        new BukkitRunnable() {
            int timeLeft = duration;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                if (timeLeft <= 0) {
                    location.getBlock().setType(Material.AIR);
                    Bukkit.broadcastMessage("§eАэродроп исчез!");
                    if (activeTasks.containsKey(EventType.AIRDROP)) {
                        activeTasks.remove(EventType.AIRDROP);
                    }
                    eventTimers.remove(EventType.AIRDROP);
                    cancel();
                    return;
                }
                eventTimers.put(EventType.AIRDROP, timeLeft);
                for (int i = 0; i < 360; i += 15) {
                    double angle = Math.toRadians(i);
                    double x = location.getX() + 1.5 * Math.cos(angle);
                    double z = location.getZ() + 1.5 * Math.sin(angle);
                    Location particleLoc = new Location(location.getWorld(), x, location.getY() + 0.5, z);
                    location.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ========================================================================
    // БОМБАРДИРОВКА
    // ========================================================================
    private void startBombardment() {
        EventSettings config = eventSettings.get(EventType.BOMBARDMENT);
        Location center = config.getCenter();
        int radius = config.getRadius();
        int tntCount = config.getTntCount();
        int tntInterval = config.getTntInterval();

        if (center == null) {
            center = Bukkit.getWorlds().get(0).getSpawnLocation();
            config.setCenter(center);
        }

        final Location finalCenter = center;
        final int finalRadius = radius;

        Bukkit.broadcastMessage("§c§lБОМБАРДИРОВКА НАЧАЛАСЬ! §eУкрывайтесь!");

        BukkitTask task = new BukkitRunnable() {
            int dropped = 0;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                if (dropped >= tntCount) {
                    cancel();
                    Bukkit.broadcastMessage("§eБомбардировка завершена!");
                    return;
                }
                try {
                    Location tntLoc = getRandomLocation(finalCenter, finalRadius);
                    tntLoc.setY(finalCenter.getY() + 25 + Math.random() * 15);

                    TNTPrimed tnt = tntLoc.getWorld().spawn(tntLoc, TNTPrimed.class);
                    tnt.setFuseTicks(60 + (int) (Math.random() * 30));
                    tnt.setVelocity(new Vector(0, -0.5, 0));

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (tnt.isDead() || tnt.isOnGround()) {
                                tnt.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, tnt.getLocation(), 20);
                                cancel();
                                return;
                            }
                            tnt.getWorld().spawnParticle(Particle.FLAME, tnt.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.01);
                            tnt.getWorld().spawnParticle(Particle.SMOKE_LARGE, tnt.getLocation().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.01);
                        }
                    }.runTaskTimer(plugin, 0L, 2L);

                    tnt.getWorld().playSound(tntLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.3f, 0.5f + (float) Math.random() * 0.5f);

                    dropped++;
                } catch (Exception e) {
                    // ignore
                }
            }
        }.runTaskTimer(plugin, 0L, tntInterval);
        activeTasks.put(EventType.BOMBARDMENT, task);
    }

    // ========================================================================
    // ЯДЕРНАЯ БОЕГОЛОВКА
    // ========================================================================
    private void startNuclear() {
        EventSettings config = eventSettings.get(EventType.NUCLEAR);
        Location center = config.getCenter();
        if (center == null) {
            center = Bukkit.getWorlds().get(0).getSpawnLocation();
            config.setCenter(center);
        }
        final float power = config.getExplosionPower();

        Bukkit.broadcastMessage("§c§lЯДЕРНАЯ БОЕГОЛОВКА! §eВзрыв через 3 секунды! Укрывайтесь!");

        NuclearBombAnimation animation = new NuclearBombAnimation(plugin, center, 1, power);
        animation.startAnimation(() -> {
            if (activeTasks.containsKey(EventType.NUCLEAR)) {
                activeTasks.remove(EventType.NUCLEAR);
            }
            Bukkit.broadcastMessage("§eЯдерный взрыв завершён!");
        });

        BukkitTask marker = new BukkitRunnable() {
            @Override
            public void run() {}
        }.runTaskTimer(plugin, 0L, 999999L);
        activeTasks.put(EventType.NUCLEAR, marker);

        new BukkitRunnable() {
            int time = 3;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                if (time <= 0) {
                    cancel();
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle("§c§l" + time, "§eДо взрыва...", 10, 20, 10);
                }
                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        eventLocations.put(EventType.NUCLEAR, center);
    }

    // ========================================================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД
    // ========================================================================
    private Location getRandomLocation(Location center, int radius) {
        World world = center.getWorld();
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        double y = world.getHighestBlockYAt((int) x, (int) z) + 1;
        return new Location(world, x, y, z);
    }

    // ========================================================================
    // ПУБЛИЧНЫЙ МЕТОД ДЛЯ ОБНОВЛЕНИЯ ТАЙМЕРОВ
    // ========================================================================
    public void updateActionBarTimers() {
        if (!isEventActive) return;

        int airdropTime = eventTimers.getOrDefault(EventType.AIRDROP, 0);
        String airdropText = "";
        if (airdropTime < 0) {
            airdropText = "§6Аэродроп через: §e" + (-airdropTime) + "с";
        } else if (airdropTime > 0) {
            airdropText = "§6Аэродроп: §e" + airdropTime + "с";
        }

        if (!airdropText.isEmpty()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendActionBar(airdropText);
            }
        }
    }

    // ========================================================================
    // СОХРАНЕНИЕ / ЗАГРУЗКА НАСТРОЕК
    // ========================================================================
    public void saveEvents() {
        try (Writer writer = new FileWriter(dataFile)) {
            Map<String, Object> dataMap = new HashMap<>();
            for (Map.Entry<EventType, EventSettings> entry : eventSettings.entrySet()) {
                EventSettings settings = entry.getValue();
                Map<String, Object> settingsMap = new HashMap<>();
                settingsMap.put("enabled", settings.isEnabled());
                settingsMap.put("delay", settings.getDelay());
                settingsMap.put("interval", settings.getInterval());
                settingsMap.put("duration", settings.getDuration());
                settingsMap.put("radius", settings.getRadius());
                settingsMap.put("tntCount", settings.getTntCount());
                settingsMap.put("tntInterval", settings.getTntInterval());
                settingsMap.put("explosionPower", settings.getExplosionPower());
                if (settings.getCenter() != null) {
                    settingsMap.put("centerWorld", settings.getCenter().getWorld().getName());
                    settingsMap.put("centerX", settings.getCenter().getX());
                    settingsMap.put("centerY", settings.getCenter().getY());
                    settingsMap.put("centerZ", settings.getCenter().getZ());
                }
                // Сохраняем лут для аэродропа
                if (settings.getType() == EventType.AIRDROP) {
                    List<Map<String, Object>> lootList = new ArrayList<>();
                    for (EventSettings.LootItem loot : settings.getLootItems()) {
                        Map<String, Object> lootMap = new HashMap<>();
                        lootMap.put("item", loot.getItem().serialize());
                        lootMap.put("chance", loot.getChance());
                        lootList.add(lootMap);
                    }
                    settingsMap.put("lootItems", lootList);
                }
                dataMap.put(entry.getKey().name(), settingsMap);
            }
            gson.toJson(dataMap, writer);
        } catch (IOException e) {
            // ignore
        }
    }

    @SuppressWarnings("unchecked")
    public void loadEvents() {
        if (!dataFile.exists()) return;
        try (Reader reader = new FileReader(dataFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                EventType type;
                try {
                    type = EventType.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                JsonObject settingsObj = entry.getValue().getAsJsonObject();
                EventSettings settings = eventSettings.get(type);
                if (settings == null) continue;
                if (settingsObj.has("enabled")) settings.setEnabled(settingsObj.get("enabled").getAsBoolean());
                if (settingsObj.has("delay")) settings.setDelay(settingsObj.get("delay").getAsInt());
                if (settingsObj.has("interval")) settings.setInterval(settingsObj.get("interval").getAsInt());
                if (settingsObj.has("duration")) settings.setDuration(settingsObj.get("duration").getAsInt());
                if (settingsObj.has("radius")) settings.setRadius(settingsObj.get("radius").getAsInt());
                if (settingsObj.has("tntCount")) settings.setTntCount(settingsObj.get("tntCount").getAsInt());
                if (settingsObj.has("tntInterval")) settings.setTntInterval(settingsObj.get("tntInterval").getAsInt());
                if (settingsObj.has("explosionPower")) settings.setExplosionPower(settingsObj.get("explosionPower").getAsFloat());
                if (settingsObj.has("centerWorld") && settingsObj.has("centerX")) {
                    World world = Bukkit.getWorld(settingsObj.get("centerWorld").getAsString());
                    if (world != null) {
                        double x = settingsObj.get("centerX").getAsDouble();
                        double y = settingsObj.get("centerY").getAsDouble();
                        double z = settingsObj.get("centerZ").getAsDouble();
                        settings.setCenter(new Location(world, x, y, z));
                    }
                }
                // Загружаем лут для аэродропа
                if (type == EventType.AIRDROP && settingsObj.has("lootItems")) {
                    settings.getLootItems().clear();
                    JsonArray lootArray = settingsObj.get("lootItems").getAsJsonArray();
                    for (JsonElement lootElem : lootArray) {
                        JsonObject lootObj = lootElem.getAsJsonObject();
                        if (lootObj.has("item") && lootObj.has("chance")) {
                            try {
                                ItemStack item = ItemStack.deserialize(
                                        gson.fromJson(lootObj.get("item"), Map.class)
                                );
                                double chance = lootObj.get("chance").getAsDouble();
                                settings.getLootItems().add(new EventSettings.LootItem(item, chance));
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            // ignore
        }
    }

    // ========================================================================
    // ВНУТРЕННИЕ КЛАССЫ
    // ========================================================================
    public static class EventSettings {
        private EventType type;
        private boolean enabled;
        private int delay;
        private int interval;
        private int duration;
        private Location center;
        private int radius;
        private List<LootItem> lootItems;
        private int tntCount;
        private int tntInterval;
        private float explosionPower = 5.0f;

        public EventSettings(EventType type) {
            this.type = type;
            this.enabled = true;
            this.delay = 60;
            this.interval = 120;
            this.duration = 120;
            this.radius = 50;
            this.lootItems = new ArrayList<>();
            this.tntCount = 20;
            this.tntInterval = 10;
        }

        public EventType getType() { return type; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getDelay() { return delay; }
        public void setDelay(int delay) { this.delay = delay; }
        public int getInterval() { return interval; }
        public void setInterval(int interval) { this.interval = interval; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public Location getCenter() { return center; }
        public void setCenter(Location center) { this.center = center; }
        public int getRadius() { return radius; }
        public void setRadius(int radius) { this.radius = radius; }
        public List<LootItem> getLootItems() { return lootItems; }
        public void setLootItems(List<LootItem> lootItems) { this.lootItems = lootItems; }
        public int getTntCount() { return tntCount; }
        public void setTntCount(int tntCount) { this.tntCount = tntCount; }
        public int getTntInterval() { return tntInterval; }
        public void setTntInterval(int tntInterval) { this.tntInterval = tntInterval; }
        public float getExplosionPower() { return explosionPower; }
        public void setExplosionPower(float power) { this.explosionPower = Math.max(0.1f, Math.min(250.0f, power)); }

        public static class LootItem {
            private ItemStack item;
            private double chance;
            public LootItem(ItemStack item, double chance) { this.item = item; this.chance = chance; }
            public ItemStack getItem() { return item; }
            public double getChance() { return chance; }
            public void setItem(ItemStack item) { this.item = item; }
            public void setChance(double chance) { this.chance = chance; }
        }
    }

    public enum EventType {
        AIRDROP,
        BOMBARDMENT,
        NUCLEAR
    }
}