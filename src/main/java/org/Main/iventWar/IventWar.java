package org.Main.iventWar;

import org.bukkit.plugin.java.JavaPlugin;

public class IventWar extends JavaPlugin {
    private static IventWar instance;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        instance = this;
        teamManager = new TeamManager(this);
        teamManager.loadTeams();
        getCommand("team").setExecutor(new CommandHandler(this));
        getCommand("topbro").setExecutor(new CommandHandler(this));
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getLogger().info("IventWar enabled!");
    }

    @Override
    public void onDisable() {
        if (teamManager != null) teamManager.saveTeams();
        getLogger().info("IventWar disabled!");
    }

    public static IventWar getInstance() { return instance; }
    public TeamManager getTeamManager() { return teamManager; }
}