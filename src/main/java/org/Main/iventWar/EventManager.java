package org.Main.iventWar;

import org.bukkit.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {
    private final IventWar plugin;

    // ---- Основные настройки ивента ----
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

    // ---- Настройки дополнительных событий ----
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
            eventSettings.put(type, new EventSettings(type));
        }
    }

    // ====================================================================
    // МЕТОДЫ ДЛЯ ОСНОВНОГО ИВЕНТА
    // ====================================================================

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
                    endMainEvent();
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

    // ====================================================================
    // МЕТОДЫ ДЛЯ НОВЫХ СОБЫТИЙ
    // ====================================================================

    public EventSettings getSettings(EventType type) {
        return eventSettings.get(type);
    }

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
            case AIRDROP:
                startAirdrop();
                break;
            case MYSTERY_TRADER:
                startMysteryTrader();
                break;
            case BOMBARDMENT:
                startBombardment();
                break;
        }
    }

    public void stopEvent(EventType type) {
        if (activeTasks.containsKey(type)) {
            activeTasks.get(type).cancel();
            activeTasks.remove(type);
            Bukkit.broadcastMessage("§cСобытие " + type.name() + " остановлено!");
        }
    }

    public Location getEventLocation(EventType type) {
        return eventLocations.get(type);
    }

    // ===== Аэродроп =====
    private void startAirdrop() {
        EventSettings config = eventSettings.get(EventType.AIRDROP);
        Location center = config.getCenter();
        int radius = config.getRadius();

        if (center == null) {
            Bukkit.broadcastMessage("§cНе установлены координаты для аэродропа!");
            return;
        }

        Location dropLocation = getRandomLocation(center, radius);
        Bukkit.broadcastMessage("§6§lАЭРОДРОП! §eЧерез 1 минуту в координатах: §6" +
                dropLocation.getBlockX() + " " + dropLocation.getBlockY() + " " + dropLocation.getBlockZ());

        BukkitTask task = new BukkitRunnable() {
            int countdown = 60;
            @Override
            public void run() {
                if (countdown <= 0) {
                    createAirdropChest(dropLocation);
                    this.cancel();
                    return;
                }
                for (int i = 0; i < 10; i++) {
                    Location particleLoc = dropLocation.clone().add(
                            (Math.random() - 0.5) * 10,
                            Math.random() * 20 + 5,
                            (Math.random() - 0.5) * 10
                    );
                    dropLocation.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 5, 0, 0, 0, 0);
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeTasks.put(EventType.AIRDROP, task);
        eventLocations.put(EventType.AIRDROP, dropLocation);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeTasks.containsKey(EventType.AIRDROP)) {
                activeTasks.remove(EventType.AIRDROP);
                Bukkit.broadcastMessage("§eАэродроп исчез!");
            }
        }, 1200L);
    }

    private void createAirdropChest(Location location) {
        location.getWorld().createExplosion(location, 0);
        location.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, location, 10);
        location.getBlock().setType(Material.CHEST);

        Inventory chest = (Inventory) location.getBlock().getState();
        List<EventSettings.LootItem> lootItems = eventSettings.get(EventType.AIRDROP).getLootItems();

        for (EventSettings.LootItem loot : lootItems) {
            if (Math.random() < loot.getChance()) {
                chest.addItem(loot.getItem());
            }
        }

        Bukkit.broadcastMessage("§a§lАЭРОДРОП ПРИБЫЛ! §eИщите сундук по координатам: §6" +
                location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
    }

    // ===== Тайный торговец =====
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

        Bukkit.broadcastMessage("§a§lТАЙНЫЙ ТОРГОВЕЦ! §eНайдите его по координатам: §6" +
                traderLoc.getBlockX() + " " + traderLoc.getBlockY() + " " + traderLoc.getBlockZ());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (eventLocations.containsKey(EventType.MYSTERY_TRADER)) {
                Location loc = eventLocations.get(EventType.MYSTERY_TRADER);
                loc.getBlock().setType(Material.AIR);
                eventLocations.remove(EventType.MYSTERY_TRADER);
                Bukkit.broadcastMessage("§eТайный торговец исчез!");
            }
        }, 1200L);
    }

    // ===== Бомбардировка (исправлена) =====
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
                if (dropped >= tntCount) {
                    this.cancel();
                    Bukkit.broadcastMessage("§eБомбардировка завершена!");
                    return;
                }

                Location tntLoc = getRandomLocation(center, radius);
                tntLoc.setY(center.getY() + 15 + Math.random() * 10);

                // Создаём TNT сущность с задержкой взрыва
                TNTPrimed tnt = tntLoc.getWorld().spawn(tntLoc, TNTPrimed.class);
                tnt.setFuseTicks(40 + (int)(Math.random() * 20)); // взрыв через 2-3 секунды
                tnt.setVelocity(tnt.getVelocity().setY(0).setX(0).setZ(0)); // не двигается

                dropped++;
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

    // ====================================================================
    // ВСПОМОГАТЕЛЬНЫЙ КЛАСС ДЛЯ НАСТРОЕК СОБЫТИЙ
    // ====================================================================

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
            this.enabled = false;
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