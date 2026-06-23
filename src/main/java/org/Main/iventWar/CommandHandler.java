package org.Main.iventWar;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final IventWar plugin;
    private final TeamManager teamManager;

    public CommandHandler(IventWar plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this!");
            return true;
        }
        if (command.getName().equalsIgnoreCase("topbro")) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Топбро жирная свинья");
            return true;
        }
        if (command.getName().equalsIgnoreCase("team")) {
            if (args.length == 0) {
                sendHelp(player);
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "create":  handleCreate(player, args); break;
                case "invite":  handleInvite(player, args); break;
                case "accept":  handleAccept(player); break;
                case "decline": handleDecline(player); break;
                case "kick":    handleKick(player, args); break;
                case "leave":   handleLeave(player); break;
                case "info":    handleInfo(player); break;
                case "desc":    handleDesc(player, args); break;
                case "color":   handleColor(player, args); break;
                case "prefix":  handlePrefix(player, args); break;
                default:        sendHelp(player); break;
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== IventWar Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/team create <name> " + ChatColor.WHITE + "- Create a team");
        player.sendMessage(ChatColor.YELLOW + "/team invite <player> " + ChatColor.WHITE + "- Invite a player");
        player.sendMessage(ChatColor.YELLOW + "/team accept " + ChatColor.WHITE + "- Accept an invitation");
        player.sendMessage(ChatColor.YELLOW + "/team decline " + ChatColor.WHITE + "- Decline an invitation");
        player.sendMessage(ChatColor.YELLOW + "/team kick <player> " + ChatColor.WHITE + "- Kick a player (Leader only)");
        player.sendMessage(ChatColor.YELLOW + "/team leave " + ChatColor.WHITE + "- Leave your team");
        player.sendMessage(ChatColor.YELLOW + "/team info " + ChatColor.WHITE + "- Show team info");
        player.sendMessage(ChatColor.YELLOW + "/team desc <description> " + ChatColor.WHITE + "- Set description (Leader only)");
        player.sendMessage(ChatColor.YELLOW + "/team color <color> " + ChatColor.WHITE + "- Set color (Leader only)");
        player.sendMessage(ChatColor.YELLOW + "/team prefix <prefix> " + ChatColor.WHITE + "- Set your prefix");
        player.sendMessage(ChatColor.GOLD + "/topbro " + ChatColor.WHITE + "- Special command");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team create <name>");
            return;
        }
        String teamName = args[1];
        if (teamManager.getTeam(teamName) != null) {
            player.sendMessage(ChatColor.RED + "Team already exists!");
            return;
        }
        if (teamManager.getPlayerTeam(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a team!");
            return;
        }
        Team team = teamManager.createTeam(teamName, player.getUniqueId());
        if (team != null) {
            player.sendMessage(ChatColor.GREEN + "Team '" + teamName + "' created!");
            player.sendMessage(ChatColor.GRAY + "You are the leader. Use /team invite to add members.");
            teamManager.updatePlayerTab(player);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to create team.");
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team invite <player>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the leader can invite!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }
        if (teamManager.getPlayerTeam(target.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "That player is already in a team!");
            return;
        }
        teamManager.addInvitation(target.getUniqueId(), team.getName());
        target.sendMessage(ChatColor.GOLD + "You have been invited to join team '" +
                team.getColoredName() + ChatColor.GOLD + "' by " + player.getName());
        target.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GREEN + "/team accept" +
                ChatColor.YELLOW + " to join or " + ChatColor.RED + "/team decline" +
                ChatColor.YELLOW + " to decline!");
        player.sendMessage(ChatColor.GREEN + "Invitation sent to " + target.getName() + "!");
    }

    private void handleAccept(Player player) {
        String teamName = teamManager.getInvitation(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatColor.RED + "No pending invitations!");
            return;
        }
        Team team = teamManager.getTeam(teamName);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "This team no longer exists!");
            teamManager.removeInvitation(player.getUniqueId());
            return;
        }
        if (teamManager.addPlayerToTeam(teamName, player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "You joined team '" + team.getColoredName() + ChatColor.GREEN + "'!");
            for (UUID member : team.getMembers()) {
                Player p = Bukkit.getPlayer(member);
                if (p != null && !p.equals(player)) {
                    p.sendMessage(ChatColor.YELLOW + player.getName() + " has joined the team!");
                }
            }
            teamManager.removeInvitation(player.getUniqueId());
            teamManager.updateAllTeamTab(team);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to join team!");
        }
    }

    private void handleDecline(Player player) {
        String teamName = teamManager.getInvitation(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatColor.RED + "No pending invitations!");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "You declined the invitation to team '" + teamName + "'");
        teamManager.removeInvitation(player.getUniqueId());
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team kick <player>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the leader can kick!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot kick yourself! Use /team leave.");
            return;
        }
        if (teamManager.kickPlayerFromTeam(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + " from the team!");
            target.sendMessage(ChatColor.RED + "You were kicked from team '" + team.getColoredName() + ChatColor.RED + "'!");
            teamManager.updatePlayerTab(target);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to kick player.");
        }
    }

    private void handleLeave(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }
        if (teamManager.removePlayerFromTeam(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You have left the team.");
            teamManager.updatePlayerTab(player);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to leave team.");
        }
    }

    private void handleInfo(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "=== Team Info ===");
        player.sendMessage(ChatColor.YELLOW + "Name: " + team.getColoredName());
        player.sendMessage(ChatColor.YELLOW + "Leader: " + Bukkit.getOfflinePlayer(team.getLeader()).getName());
        player.sendMessage(ChatColor.YELLOW + "Color: " + team.getColor() + team.getColor().name());
        player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE +
                (team.getDescription().isEmpty() ? "No description" : team.getDescription()));
        player.sendMessage(ChatColor.YELLOW + "Members (" + team.getMemberCount() + "):");
        for (UUID member : team.getMembers()) {
            String name = Bukkit.getOfflinePlayer(member).getName();
            String prefix = team.getPrefix(member);
            String role = member.equals(team.getLeader()) ? ChatColor.GOLD + "Leader" : ChatColor.GRAY + "Member";
            player.sendMessage(ChatColor.YELLOW + "  - " + role + ChatColor.WHITE + ": " + name +
                    (prefix.isEmpty() ? "" : ChatColor.GRAY + " (" + prefix + ")"));
        }
    }

    private void handleDesc(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the leader can set description!");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team desc <description>");
            return;
        }
        StringBuilder desc = new StringBuilder();
        for (int i = 1; i < args.length; i++) desc.append(args[i]).append(" ");
        team.setDescription(desc.toString().trim());
        teamManager.saveTeams();
        player.sendMessage(ChatColor.GREEN + "Team description updated!");
    }

    private void handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team color <color>");
            player.sendMessage(ChatColor.YELLOW + "Colors: red, green, blue, yellow, gold, aqua, dark_red, dark_green, dark_blue, dark_purple, black, white, gray");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the leader can set color!");
            return;
        }
        ChatColor color;
        try {
            color = ChatColor.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid color! Use: red, green, blue, yellow, gold, aqua, dark_red, dark_green, dark_blue, dark_purple, black, white, gray");
            return;
        }
        team.setColor(color);
        teamManager.saveTeams();
        teamManager.updateAllTeamTab(team);
        player.sendMessage(ChatColor.GREEN + "Team color updated to " + color + color.name() + ChatColor.GREEN + "!");
    }

    private void handlePrefix(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team prefix <prefix>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }
        StringBuilder pref = new StringBuilder();
        for (int i = 1; i < args.length; i++) pref.append(args[i]).append(" ");
        String prefix = pref.toString().trim();
        team.setPrefix(player.getUniqueId(), prefix);
        teamManager.saveTeams();
        teamManager.updatePlayerTab(player);
        player.sendMessage(ChatColor.GREEN + "Your prefix set to: " + ChatColor.WHITE + prefix);
    }
}