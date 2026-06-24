package org.Main.iventWar;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {
    private final IventWar plugin;
    private final TeamManager teamManager;
    private final ZoneManager zoneManager;
    private final AdminHubGUI adminHubGUI;

    public EventListener(IventWar plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.zoneManager = plugin.getZoneManager();
        this.adminHubGUI = plugin.getAdminHubGUI();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Задержка 2 тика для полной загрузки игрока
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Team team = teamManager.getPlayerTeam(player.getUniqueId());
            if (team != null) {
                teamManager.updatePlayerDisplay(player);
            }
        }, 2L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        for (Zone zone : zoneManager.getZones()) {
            zone.removePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        Zone currentZone = zoneManager.getZoneAt(to);
        Zone oldZone = zoneManager.getZoneAt(event.getFrom());

        if (currentZone == null && oldZone != null) {
            oldZone.removePlayer(player);
        } else if (currentZone != null && oldZone == null) {
            currentZone.addPlayer(player);
            String owner = currentZone.getOwnerTeam();
            String ownerDisplay = (owner == null) ? "§7Нейтральная" : "§6" + owner;
            player.sendMessage("§aВы вошли в зону §6" + currentZone.getName() + " §a| Владелец: " + ownerDisplay);
        } else if (currentZone != null && oldZone != null && !currentZone.equals(oldZone)) {
            oldZone.removePlayer(player);
            currentZone.addPlayer(player);
            String owner = currentZone.getOwnerTeam();
            String ownerDisplay = (owner == null) ? "§7Нейтральная" : "§6" + owner;
            player.sendMessage("§aВы вошли в зону §6" + currentZone.getName() + " §a| Владелец: " + ownerDisplay);
        }
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        String messageText = ((TextComponent) event.message()).content();
        event.setCancelled(true);

        ChatColor playerColor = adminHubGUI.getPlayerChatColor(player);
        String playerNameColor = playerColor + player.getName();

        String formatted;
        if (team != null) {
            formatted = team.getColoredNameWithBrackets() + " " + playerNameColor + ": " + messageText;
        } else {
            formatted = playerNameColor + ": " + messageText;
        }
        Component finalMessage = Component.text(formatted);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(finalMessage);
        }
    }
}