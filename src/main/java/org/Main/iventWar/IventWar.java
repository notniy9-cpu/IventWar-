package org.Main.iventWar;

import org.bukkit.plugin.java.JavaPlugin;

public class IventWar extends JavaPlugin {
    private static IventWar instance;
    private TeamManager teamManager;
    private EventManager eventManager;
    private ZoneManager zoneManager;

    @Override
    public void onEnable() {
        instance = this;

        teamManager = new TeamManager(this);
        teamManager.loadTeams();

        eventManager = new EventManager(this);
        zoneManager = new ZoneManager(this);

        getCommand("team").setExecutor(new CommandHandler(this));
        getCommand("my").setExecutor(new CommandHandler(this));
        getCommand("myteam").setExecutor(new CommandHandler(this));
        getCommand("tc").setExecutor(new CommandHandler(this));
        getCommand("topbro").setExecutor(new CommandHandler(this));
        getCommand("startevent").setExecutor(new CommandHandler(this));
        getCommand("startivent").setExecutor(new CommandHandler(this));
        getCommand("closeevent").setExecutor(new CommandHandler(this));
        getCommand("createzone").setExecutor(new CommandHandler(this));
        getCommand("zone").setExecutor(new CommandHandler(this)); // НОВАЯ КОМАНДА

        getServer().getPluginManager().registerEvents(new EventListener(this), this);

        getLogger().info("IventWar включён!");
        getLogger().info("Загружено зон: " + zoneManager.getZones().size());
    }

    @Override
    public void onDisable() {
        if (teamManager != null) teamManager.saveTeams();
        if (zoneManager != null) zoneManager.saveZones();
        getLogger().info("IventWar выключен!");
    }

    public static IventWar getInstance() { return instance; }
    public TeamManager getTeamManager() { return teamManager; }
    public EventManager getEventManager() { return eventManager; }
    public ZoneManager getZoneManager() { return zoneManager; }
}