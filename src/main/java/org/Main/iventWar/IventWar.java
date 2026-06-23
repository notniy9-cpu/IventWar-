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
        getCommand("my").setExecutor(new CommandHandler(this));
        getCommand("myteam").setExecutor(new CommandHandler(this));
        getCommand("tc").setExecutor(new CommandHandler(this));
        getCommand("topbro").setExecutor(new CommandHandler(this));
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getLogger().info("IventWar включён!");
    }

    @Override
    public void onDisable() {
        if (teamManager != null) teamManager.saveTeams();
        getLogger().info("IventWar выключен!");
    }

    public static IventWar getInstance() { return instance; }
    public TeamManager getTeamManager() { return teamManager; }
}