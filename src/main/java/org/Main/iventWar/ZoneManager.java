package org.Main.iventWar;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneManager {
    private final IventWar plugin;
    private final Map<String, Zone> zones;
    private final File dataFile;
    private final Gson gson;

    public ZoneManager(IventWar plugin) {
        this.plugin = plugin;
        this.zones = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "zones.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        loadZones();
        startParticleTask();
        startCaptureTask();
    }

    public void createZone(String name, Location center, int radius, int height, int captureTime) {
        if (zones.containsKey(name)) return;
        zones.put(name, new Zone(name, center, radius, height, captureTime));
        saveZones();
    }

    public void deleteZone(String name) {
        zones.remove(name);
        saveZones();
    }

    public Zone getZone(String name) {
        return zones.get(name);
    }

    public Collection<Zone> getZones() {
        return zones.values();
    }

    public Zone getZoneAt(Location loc) {
        for (Zone zone : zones.values()) {
            if (zone.isInside(loc)) return zone;
        }
        return null;
    }

    // ===== ЗАХВАТ ЗОНЫ (упрощённая логика) =====
    private void startCaptureTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                for (Zone zone : zones.values()) {
                    if (zone.getPlayersInZone().isEmpty()) continue;

                    // Собираем игроков по командам в зоне
                    Map<String, List<UUID>> playersByTeam = new HashMap<>();
                    for (UUID uuid : zone.getPlayersInZone()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null || !p.isOnline()) continue;
                        Team team = plugin.getTeamManager().getPlayerTeam(uuid);
                        String teamName = (team != null) ? team.getName() : null;
                        if (teamName == null) continue;
                        playersByTeam.computeIfAbsent(teamName, k -> new ArrayList<>()).add(uuid);
                    }

                    if (playersByTeam.isEmpty()) continue;

                    // Находим доминирующую команду (с наибольшим количеством)
                    String dominantTeam = null;
                    int maxCount = 0;
                    for (Map.Entry<String, List<UUID>> entry : playersByTeam.entrySet()) {
                        if (entry.getValue().size() > maxCount) {
                            maxCount = entry.getValue().size();
                            dominantTeam = entry.getKey();
                        }
                    }

                    // Если зона уже принадлежит этой команде – сбрасываем прогресс
                    if (zone.getOwnerTeam() != null && zone.getOwnerTeam().equals(dominantTeam)) {
                        zone.clearProgress();
                        continue;
                    }

                    // Проверяем, есть ли игроки других команд в зоне
                    boolean hasOtherTeams = false;
                    for (String team : playersByTeam.keySet()) {
                        if (!team.equals(dominantTeam)) {
                            hasOtherTeams = true;
                            break;
                        }
                    }

                    // Если есть другие команды – захват НЕ идёт (нейтральная зона)
                    if (hasOtherTeams) {
                        zone.clearProgress();
                        continue;
                    }

                    // Если в зоне только одна команда (или доминирующая) – начинаем захват
                    for (UUID uuid : playersByTeam.get(dominantTeam)) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null) continue;
                        int progress = zone.getProgress(p);
                        int newProgress = Math.min(progress + 1, zone.getCaptureTime());
                        zone.setProgress(p, newProgress);

                        // Показываем прогресс всем игрокам в зоне
                        for (UUID allUuid : zone.getPlayersInZone()) {
                            Player allP = Bukkit.getPlayer(allUuid);
                            if (allP != null && allP.isOnline()) {
                                int percent = (newProgress * 100) / zone.getCaptureTime();
                                String owner = zone.getOwnerTeam();
                                String ownerDisplay = (owner == null) ? "§7Нейтральная" : "§6" + owner;
                                allP.sendActionBar("§fЗона: §6" + zone.getName() + " §f| Владелец: " + ownerDisplay +
                                        " §f| Захват: §e" + percent + "%");
                            }
                        }

                        if (newProgress >= zone.getCaptureTime()) {
                            zone.setOwnerTeam(dominantTeam);
                            zone.clearProgress();
                            Team team = plugin.getTeamManager().getTeam(dominantTeam);
                            String colorName = (team != null) ? team.getColor().toString() : "§a";
                            Bukkit.broadcastMessage("§a§lЗОНА ЗАХВАЧЕНА! " + colorName + dominantTeam + " §aзахватила зону '" + zone.getName() + "'!");
                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ===== ЧАСТИЦЫ (без изменений) =====
    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin == null || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                try {
                    for (Zone zone : zones.values()) {
                        Location center = zone.getCenter();
                        World world = center.getWorld();
                        if (world == null) continue;

                        Particle particle = Particle.END_ROD;
                        String ownerTeam = zone.getOwnerTeam();
                        if (ownerTeam != null) {
                            Team team = plugin.getTeamManager().getTeam(ownerTeam);
                            if (team != null) {
                                switch (team.getColor()) {
                                    case RED:
                                    case DARK_RED:
                                        particle = Particle.REDSTONE;
                                        break;
                                    case BLUE:
                                    case DARK_BLUE:
                                        particle = Particle.SPELL_MOB;
                                        break;
                                    case GREEN:
                                    case DARK_GREEN:
                                        particle = Particle.SPELL_MOB;
                                        break;
                                    case GOLD:
                                    case YELLOW:
                                        particle = Particle.SPELL_MOB;
                                        break;
                                    case AQUA:
                                    case DARK_AQUA:
                                        particle = Particle.SPELL_MOB;
                                        break;
                                    case LIGHT_PURPLE:
                                    case DARK_PURPLE:
                                        particle = Particle.SPELL_MOB;
                                        break;
                                    default:
                                        particle = Particle.END_ROD;
                                        break;
                                }
                            }
                        }

                        int height = zone.getHeight();
                        int radius = zone.getRadius();
                        for (int y = 0; y <= height; y += 1) {
                            for (int i = 0; i < 360; i += 10) {
                                double angle = Math.toRadians(i);
                                double x = center.getX() + radius * Math.cos(angle);
                                double z = center.getZ() + radius * Math.sin(angle);
                                Location loc = new Location(world, x, center.getY() + y, z);
                                world.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
                            }
                        }

                        for (UUID uuid : zone.getPlayersInZone()) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p == null || !p.isOnline()) continue;
                            if (!zone.isInside(p.getLocation())) continue;

                            String owner = zone.getOwnerTeam();
                            int progress = zone.getProgress(p);
                            int percent = (progress * 100) / zone.getCaptureTime();
                            String ownerDisplay = (owner == null) ? "§7Нейтральная" : "§6" + owner;
                            String progressDisplay = (progress > 0) ? " §f| Захват: §e" + percent + "%" : "";
                            p.sendActionBar("§fЗона: §6" + zone.getName() + " §f| Владелец: " + ownerDisplay + progressDisplay);
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // ----- Сохранение/Загрузка (без изменений) -----
    public void saveZones() {
        try (Writer writer = new FileWriter(dataFile)) {
            Map<String, ZoneData> dataMap = new HashMap<>();
            for (Map.Entry<String, Zone> entry : zones.entrySet()) {
                Zone zone = entry.getValue();
                ZoneData data = new ZoneData(
                        zone.getCenter().getWorld().getName(),
                        zone.getCenter().getX(), zone.getCenter().getY(), zone.getCenter().getZ(),
                        zone.getRadius(), zone.getHeight(), zone.getCaptureTime(), zone.getOwnerTeam()
                );
                dataMap.put(entry.getKey(), data);
            }
            gson.toJson(dataMap, writer);
        } catch (IOException e) {
            // ignore
        }
    }

    public void loadZones() {
        if (!dataFile.exists()) return;
        try (Reader reader = new FileReader(dataFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String name = entry.getKey();
                JsonObject data = entry.getValue().getAsJsonObject();
                World world = Bukkit.getWorld(data.get("world").getAsString());
                if (world == null) continue;
                Location center = new Location(world, data.get("x").getAsDouble(), data.get("y").getAsDouble(), data.get("z").getAsDouble());
                int radius = data.get("radius").getAsInt();
                int height = data.has("height") ? data.get("height").getAsInt() : 5;
                int captureTime = data.get("captureTime").getAsInt();
                Zone zone = new Zone(name, center, radius, height, captureTime);
                if (data.has("owner") && !data.get("owner").isJsonNull()) {
                    zone.setOwnerTeam(data.get("owner").getAsString());
                }
                zones.put(name, zone);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private static class ZoneData {
        private final String world;
        private final double x, y, z;
        private final int radius, height, captureTime;
        private final String owner;
        public ZoneData(String world, double x, double y, double z, int radius, int height, int captureTime, String owner) {
            this.world = world; this.x = x; this.y = y; this.z = z;
            this.radius = radius; this.height = height; this.captureTime = captureTime; this.owner = owner;
        }
    }
}