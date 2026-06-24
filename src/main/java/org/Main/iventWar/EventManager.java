package org.Main.iventWar;

import org.bukkit.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

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

    private final Map<EventType, EventSettings> eventSettings;
    private final Map<EventType, BukkitTask> activeTasks;
    private final Map<EventType, Location> eventLocations;

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

        for (EventType type : EventType.values()) {
            EventSettings settings = new EventSettings(type);
            settings.setEnabled(true);
            eventSettings.put(type, settings);
        }
    }

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

        countdownTask = new BukkitRunnable() {
            int count = 5;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) { cancel(); return; }
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
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
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
        eventTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) { cancel(); return; }
                if (remainingSeconds <= 0) {
                    endMainEvent();
                    cancel();
                    return;
                }
                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startActionBarTimer() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) actionBarTask.cancel();
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) { cancel(); return; }
                if (!isEventActive) { cancel(); return; }
                int totalSeconds = remainingSeconds;
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                String timeStr = String.format("§6§l⏳ %02d:%02d", minutes, seconds);
                for (Player p : Bukkit.getOnlinePlayers()) p.sendActionBar(timeStr);
            }
        }.runTaskTimer(plugin, 0L, 20L);
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

    private void endMainEvent() {
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

    // ===== НОВЫЕ СОБЫТИЯ =====
    public EventSettings getSettings(EventType type) { return eventSettings.get(type); }

    public boolean isEventActive(EventType type) {
        return activeTasks.containsKey(type) && !activeTasks.get(type).isCancelled();
    }

    public void startEvent(EventType type) {
        if (isEventActive(type)) {
            Bukkit.broadcastMessage("§cСобытие " + type.name() + " уже активно!");
            return;
        }
        EventSettings config = eventSettings.get(type);
        if (!config.isEnabled()) {
            Bukkit.broadcastMessage("§cСобытие " + type.name() + " не включено!");
            return;
        }
        switch (type) {
            case AIRDROP: startAirdrop(); break;
            case MYSTERY_TRADER: startMysteryTrader(); break;
            case BOMBARDMENT: startBombardment(); break;
        }
    }

    public void stopEvent(EventType type) {
        if (activeTasks.containsKey(type)) {
            activeTasks.get(type).cancel();
            activeTasks.remove(type);
            Bukkit.broadcastMessage("§cСобытие " + type.name() + " остановлено!");
        }
    }

    public Location getEventLocation(EventType type) { return eventLocations.get(type); }

    // ===== АЭРОДРОП (с подсветкой, звуком и таймером) =====
    private void startAirdrop() {
        EventSettings config = eventSettings.get(EventType.AIRDROP);
        Location center = config.getCenter();
        int radius = config.getRadius();
        if (center == null) {
            Bukkit.broadcastMessage("§cНе установлены координаты для аэродропа!");
            return;
        }

        Location dropLocation = getRandomLocation(center, radius);
        Location groundLocation = dropLocation.clone();
        groundLocation.setY(groundLocation.getWorld().getHighestBlockYAt(groundLocation) + 1);

        // ОДНО сообщение за минуту
        Bukkit.broadcastMessage("§6§lАЭРОДРОП! §eЧерез 1 минуту по координатам: §6" +
                groundLocation.getBlockX() + " " + groundLocation.getBlockY() + " " + groundLocation.getBlockZ());

        // Подсветка места падения (золотые частицы)
        BukkitTask glowTask = new BukkitRunnable() {
            int countdown = 60;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) { cancel(); return; }
                if (countdown <= 0) {
                    spawnFallingChest(groundLocation);
                    // Через 2 секунды появляется сундук
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        createAirdropChest(groundLocation);
                        // Запускаем таймер исчезновения с отображением над сундуком
                        startChestTimer(groundLocation);
                    }, 40L);
                    cancel();
                    return;
                }
                // Подсветка контуров места падения
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = groundLocation.getX() + 3 * Math.cos(angle);
                    double z = groundLocation.getZ() + 3 * Math.sin(angle);
                    Location particleLoc = new Location(groundLocation.getWorld(), x, groundLocation.getY(), z);
                    groundLocation.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        activeTasks.put(EventType.AIRDROP, glowTask);
        eventLocations.put(EventType.AIRDROP, groundLocation);
    }

    private void spawnFallingChest(Location location) {
        World world = location.getWorld();
        Location spawnLoc = location.clone().add(0, 25, 0);
        FallingBlock chest = world.spawnFallingBlock(spawnLoc, Material.CHEST.createBlockData());
        chest.setDropItem(false);

        // Анимация падения с частицами
        new BukkitRunnable() {
            @Override
            public void run() {
                if (chest.isDead() || chest.isOnGround()) {
                    // Звук взрыва при падении
                    world.playSound(chest.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    world.spawnParticle(Particle.EXPLOSION_NORMAL, chest.getLocation(), 30);
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.FLAME, chest.getLocation().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void createAirdropChest(Location location) {
        try {
            location.getBlock().setType(Material.CHEST);
            Inventory chest = (Inventory) location.getBlock().getState();
            List<EventSettings.LootItem> lootItems = eventSettings.get(EventType.AIRDROP).getLootItems();
            for (EventSettings.LootItem loot : lootItems) {
                if (Math.random() < loot.getChance()) {
                    chest.addItem(loot.getItem());
                }
            }
            // ОДНО сообщение о прибытии
            Bukkit.broadcastMessage("§a§lАЭРОДРОП ПРИБЫЛ! §eПо координатам: §6" +
                    location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
        } catch (Exception e) { /* ignore */ }
    }

    // Таймер над сундуком до исчезновения (60 сек)
    private void startChestTimer(Location location) {
        new BukkitRunnable() {
            int timeLeft = 60;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) { cancel(); return; }
                if (timeLeft <= 0) {
                    // Исчезновение сундука
                    location.getBlock().setType(Material.AIR);
                    Bukkit.broadcastMessage("§eАэродроп исчез!");
                    if (activeTasks.containsKey(EventType.AIRDROP)) {
                        activeTasks.remove(EventType.AIRDROP);
                    }
                    cancel();
                    return;
                }
                // Отображаем таймер над сундуком
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar("§6§lАэродроп исчезнет через: §e" + timeLeft + " сек");
                }
                // Подсветка контуров сундука
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

    // ===== ТАЙНЫЙ ТОРГОВЕЦ (с подсветкой контуров) =====
    private void startMysteryTrader() {
        EventSettings config = eventSettings.get(EventType.MYSTERY_TRADER);
        Location center = config.getCenter();
        int radius = config.getRadius();
        if (center == null) {
            Bukkit.broadcastMessage("§cНе установлены координаты для торговца!");
            return;
        }

        Location traderLoc = getRandomLocation(center, radius);
        traderLoc.getBlock().setType(Material.VILLAGER_SPAWN_EGG);
        eventLocations.put(EventType.MYSTERY_TRADER, traderLoc);

        // ОДНО сообщение
        Bukkit.broadcastMessage("§a§lТАЙНЫЙ ТОРГОВЕЦ! §eПоявился по координатам: §6" +
                traderLoc.getBlockX() + " " + traderLoc.getBlockY() + " " + traderLoc.getBlockZ());

        // Подсветка контуров торговца (белые частицы)
        BukkitTask glowTask = new BukkitRunnable() {
            int timeLeft = 60;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) { cancel(); return; }
                if (!eventLocations.containsKey(EventType.MYSTERY_TRADER)) {
                    cancel();
                    return;
                }
                Location loc = eventLocations.get(EventType.MYSTERY_TRADER);

                // Подсветка контуров (круг)
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = loc.getX() + 2 * Math.cos(angle);
                    double z = loc.getZ() + 2 * Math.sin(angle);
                    Location particleLoc = new Location(loc.getWorld(), x, loc.getY() + 0.5, z);
                    loc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
                // Искры над головой
                loc.getWorld().spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 2.5, 0), 10, 0.3, 0.3, 0.3, 0);

                // Таймер исчезновения в ActionBar
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar("§d§lТорговец исчезнет через: §e" + timeLeft + " сек");
                }
                timeLeft--;
                if (timeLeft <= 0) {
                    // Исчезновение
                    loc.getBlock().setType(Material.AIR);
                    eventLocations.remove(EventType.MYSTERY_TRADER);
                    Bukkit.broadcastMessage("§eТайный торговец исчез!");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        activeTasks.put(EventType.MYSTERY_TRADER, glowTask);
    }

    // ===== БОМБАРДИРОВКА =====
    private void startBombardment() {
        EventSettings config = eventSettings.get(EventType.BOMBARDMENT);
        Location center = config.getCenter();
        int radius = config.getRadius();
        int tntCount = config.getTntCount();
        int tntInterval = config.getTntInterval();

        if (center == null) {
            Bukkit.broadcastMessage("§cНе установлены координаты для бомбардировки!");
            return;
        }

        Bukkit.broadcastMessage("§c§lБОМБАРДИРОВКА! §eУкрывайтесь!");

        BukkitTask task = new BukkitRunnable() {
            int dropped = 0;
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) { cancel(); return; }
                if (dropped >= tntCount) {
                    cancel();
                    Bukkit.broadcastMessage("§eБомбардировка завершена!");
                    return;
                }
                try {
                    Location tntLoc = getRandomLocation(center, radius);
                    tntLoc.setY(center.getY() + 15 + Math.random() * 10);
                    TNTPrimed tnt = tntLoc.getWorld().spawn(tntLoc, TNTPrimed.class);
                    tnt.setFuseTicks(40 + (int)(Math.random() * 20));
                    tnt.setVelocity(new Vector(0, 0, 0));
                    dropped++;
                } catch (Exception e) { /* ignore */ }
            }
        }.runTaskTimer(plugin, 0L, tntInterval);
        activeTasks.put(EventType.BOMBARDMENT, task);
    }

    private Location getRandomLocation(Location center, int radius) {
        World world = center.getWorld();
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        double y = world.getHighestBlockYAt((int) x, (int) z) + 1;
        return new Location(world, x, y, z);
    }

    // ===== КЛАССЫ НАСТРОЕК =====
    public static class EventSettings {
        private EventType type;
        private boolean enabled;
        private int delay;
        private int interval;
        private int duration;
        private Location center;
        private int radius;
        private List<LootItem> lootItems;
        private List<TradeItem> tradeItems;
        private int tntCount;
        private int tntInterval;

        public EventSettings(EventType type) {
            this.type = type;
            this.enabled = true;
            this.delay = 60;
            this.interval = 120;
            this.duration = 60;
            this.radius = 50;
            this.lootItems = new ArrayList<>();
            this.tradeItems = new ArrayList<>();
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
        public List<TradeItem> getTradeItems() { return tradeItems; }
        public void setTradeItems(List<TradeItem> tradeItems) { this.tradeItems = tradeItems; }
        public int getTntCount() { return tntCount; }
        public void setTntCount(int tntCount) { this.tntCount = tntCount; }
        public int getTntInterval() { return tntInterval; }
        public void setTntInterval(int tntInterval) { this.tntInterval = tntInterval; }

        public static class LootItem {
            private ItemStack item;
            private double chance;
            public LootItem(ItemStack item, double chance) { this.item = item; this.chance = chance; }
            public ItemStack getItem() { return item; }
            public double getChance() { return chance; }
            public void setItem(ItemStack item) { this.item = item; }
            public void setChance(double chance) { this.chance = chance; }
        }

        public static class TradeItem {
            private ItemStack buyItem;
            private int buyAmount;
            private ItemStack sellItem;
            private int sellAmount;
            public TradeItem(ItemStack buyItem, int buyAmount, ItemStack sellItem, int sellAmount) {
                this.buyItem = buyItem; this.buyAmount = buyAmount; this.sellItem = sellItem; this.sellAmount = sellAmount;
            }
            public ItemStack getBuyItem() { return buyItem; }
            public int getBuyAmount() { return buyAmount; }
            public ItemStack getSellItem() { return sellItem; }
            public int getSellAmount() { return sellAmount; }
        }
    }

    public enum EventType {
        AIRDROP,
        MYSTERY_TRADER,
        BOMBARDMENT
    }
}