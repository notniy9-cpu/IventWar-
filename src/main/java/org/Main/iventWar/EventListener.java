package org.Main.iventWar;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {
    private final IventWar plugin;
    private final TeamManager teamManager;

    public EventListener(IventWar plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team != null) {
            teamManager.updatePlayerTab(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // ничего не делаем
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        String messageText = ((TextComponent) event.message()).content();
        event.setCancelled(true);

        String formatted;
        if (team != null) {
            formatted = team.getColoredNameWithBrackets() + " " + player.getName() + ": " + messageText;
        } else {
            formatted = ChatColor.WHITE + player.getName() + ": " + messageText;
        }
        Component finalMessage = Component.text(formatted);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(finalMessage);
        }
    }
}