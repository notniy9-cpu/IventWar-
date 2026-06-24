package org.Main.iventWar;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;

public class Zone {
    private final String name;
    private Location center;
    private int radius;
    private int height;
    private int captureTime;
    private String ownerTeam;
    private final Map<UUID, Integer> captureProgress;
    private final Set<UUID> playersInZone;

    public Zone(String name, Location center, int radius, int height, int captureTime) {
        this.name = name;
        this.center = center;
        this.radius = radius;
        this.height = height;
        this.captureTime = captureTime;
        this.ownerTeam = null;
        this.captureProgress = new HashMap<>();
        this.playersInZone = new HashSet<>();
    }

    public String getName() { return name; }
    public Location getCenter() { return center; }
    public int getRadius() { return radius; }
    public int getHeight() { return height; }
    public int getCaptureTime() { return captureTime; }
    public String getOwnerTeam() { return ownerTeam; }
    public void setOwnerTeam(String team) { this.ownerTeam = team; }
    public Set<UUID> getPlayersInZone() { return playersInZone; }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(center.getWorld())) return false;
        double dx = loc.getX() - center.getX();
        double dz = loc.getZ() - center.getZ();
        double dy = loc.getY() - center.getY();
        return (dx * dx + dz * dz) <= (radius * radius) && dy >= 0 && dy <= height;
    }

    public void addPlayer(Player player) {
        playersInZone.add(player.getUniqueId());
        if (!captureProgress.containsKey(player.getUniqueId())) captureProgress.put(player.getUniqueId(), 0);
    }

    public void removePlayer(Player player) {
        playersInZone.remove(player.getUniqueId());
        captureProgress.remove(player.getUniqueId());
    }

    public int getProgress(Player player) {
        return captureProgress.getOrDefault(player.getUniqueId(), 0);
    }

    public void setProgress(Player player, int progress) {
        captureProgress.put(player.getUniqueId(), progress);
    }

    public void clearProgress() { captureProgress.clear(); }
    public boolean isCaptured() { return ownerTeam != null; }
    public void reset() { ownerTeam = null; captureProgress.clear(); }

    // Методы для редактирования
    public void setCenter(Location center) { this.center = center; }
    public void setRadius(int radius) { this.radius = radius; }
    public void setHeight(int height) { this.height = height; }
    public void setCaptureTime(int captureTime) { this.captureTime = captureTime; }
}