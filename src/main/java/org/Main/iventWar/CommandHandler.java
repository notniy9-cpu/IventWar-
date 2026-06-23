package org.Main.iventWar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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
            sender.sendMessage(ChatColor.RED + "Только игроки могут использовать эту команду!");
            return true;
        }

        String cmd = command.getName().toLowerCase();
        plugin.getLogger().info("Команда: " + cmd + " от " + player.getName());

        if (cmd.equals("topbro")) {
            handleTopBro(player);
            return true;
        }
        if (cmd.equals("tc")) {
            handleTeamChat(player, args);
            return true;
        }
        if (cmd.equals("myteam")) {
            handleMyTeam(player);
            return true;
        }
        if (cmd.equals("team")) {
            if (args.length == 0) {
                sendHelp(player);
                return true;
            }
            String sub = args[0].toLowerCase();
            // Быстрые стили
            if (sub.equals("bold") || sub.equals("italic") || sub.equals("underline") || sub.equals("reset")) {
                handleStyle(player, sub);
                return true;
            }
            switch (sub) {
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
                case "style":   if (args.length > 1) handleStyle(player, args[1]); else handleStyle(player, ""); break;
                default:        sendHelp(player); break;
            }
            return true;
        }
        return true;
    }

    private void handleTopBro(Player player) {
        for (int i = 0; i < 20; i++) player.sendMessage("");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "═══════════════════════════════════════════════");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "        ТОПБРО ЖИРНАЯ СВИНЬЯ        ");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "═══════════════════════════════════════════════");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < 20; i++) player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "Ты просто топ!");
        }, 40L);
    }

    private void handleTeamChat(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Использование: /tc <сообщение>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не состоишь в команде!");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String arg : args) sb.append(arg).append(" ");
        String msg = sb.toString().trim();
        String formatted = ChatColor.GOLD + "[Team] " + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + msg;
        for (UUID member : team.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) p.sendMessage(formatted);
        }
    }

    private void handleMyTeam(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "К сожалению, ты не состоишь в команде!");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "╔═══════════════════════════════════╗");
        player.sendMessage(ChatColor.GOLD + "║      " + ChatColor.BOLD + "ТВОЯ КОМАНДА" + ChatColor.GOLD + "      ║");
        player.sendMessage(ChatColor.GOLD + "╠═══════════════════════════════════╣");
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Название: " + ChatColor.WHITE + team.getColoredNameWithBrackets());
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Лидер: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(team.getLeader()).getName());
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Цвет: " + team.getColor() + team.getColor().name());
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Стиль: " + ChatColor.WHITE + getStyleName(team.getTextFormat()));
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Описание: " + ChatColor.WHITE +
                (team.getDescription().isEmpty() ? "Нет описания" : team.getDescription()));
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Участников: " + ChatColor.WHITE + team.getMemberCount());
        player.sendMessage(ChatColor.GOLD + "╠═══════════════════════════════════╣");
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Участники:");
        for (UUID member : team.getMembers()) {
            String name = Bukkit.getOfflinePlayer(member).getName();
            String prefix = team.getPrefix(member);
            String role = member.equals(team.getLeader()) ? ChatColor.GOLD + "★ Лидер" : ChatColor.GRAY + "• Участник";
            player.sendMessage(ChatColor.GOLD + "║   " + role + ChatColor.WHITE + ": " + name + " " + prefix);
        }
        player.sendMessage(ChatColor.GOLD + "╚═══════════════════════════════════╝");
        if (team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Ты лидер! Используй /team для управления.");
        }
    }

    private void handleStyle(Player player, String style) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер может менять стиль!");
            return;
        }
        if (style.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Использование: /team style <bold|italic|underline|reset>");
            player.sendMessage(ChatColor.YELLOW + "Или быстрая команда: /team bold, /team italic и т.д.");
            return;
        }
        ChatColor format;
        String styleName;
        switch (style.toLowerCase()) {
            case "bold":
                format = ChatColor.BOLD;
                styleName = "ЖИРНЫЙ";
                break;
            case "italic":
                format = ChatColor.ITALIC;
                styleName = "КУРСИВ";
                break;
            case "underline":
                format = ChatColor.UNDERLINE;
                styleName = "ПОДЧЁРКНУТЫЙ";
                break;
            case "reset":
                format = ChatColor.RESET;
                styleName = "ОБЫЧНЫЙ";
                break;
            default:
                player.sendMessage(ChatColor.RED + "Неверный стиль! Используй: bold, italic, underline, reset");
                return;
        }
        team.setTextFormat(format);
        teamManager.saveTeams();
        // Обновляем Tab для всех участников
        teamManager.updateAllTeamTab(team);
        player.sendMessage(ChatColor.GREEN + "Стиль команды изменён на: " + format + styleName);
        player.sendMessage(ChatColor.GRAY + "Пример в Tab: " + team.getColoredNameWithBrackets() + ChatColor.GRAY + " теперь так выглядит.");
    }

    private String getStyleName(ChatColor format) {
        if (format == ChatColor.BOLD) return "Жирный";
        if (format == ChatColor.ITALIC) return "Курсив";
        if (format == ChatColor.UNDERLINE) return "Подчёркнутый";
        return "Обычный";
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== IventWar Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/team create <name> " + ChatColor.WHITE + "- Создать команду");
        player.sendMessage(ChatColor.YELLOW + "/team invite <player> " + ChatColor.WHITE + "- Пригласить");
        player.sendMessage(ChatColor.YELLOW + "/team accept " + ChatColor.WHITE + "- Принять приглашение");
        player.sendMessage(ChatColor.YELLOW + "/team decline " + ChatColor.WHITE + "- Отклонить");
        player.sendMessage(ChatColor.YELLOW + "/team kick <player> " + ChatColor.WHITE + "- Исключить (Лидер)");
        player.sendMessage(ChatColor.YELLOW + "/team leave " + ChatColor.WHITE + "- Покинуть");
        player.sendMessage(ChatColor.YELLOW + "/team info " + ChatColor.WHITE + "- Информация");
        player.sendMessage(ChatColor.YELLOW + "/team desc <text> " + ChatColor.WHITE + "- Описание (Лидер)");
        player.sendMessage(ChatColor.YELLOW + "/team color <color> " + ChatColor.WHITE + "- Цвет (Лидер)");
        player.sendMessage(ChatColor.YELLOW + "/team style <bold|italic|underline|reset> " + ChatColor.WHITE + "- Стиль (Лидер)");
        player.sendMessage(ChatColor.YELLOW + "/team bold " + ChatColor.WHITE + "- Жирный (Лидер)");
        player.sendMessage(ChatColor.YELLOW + "/team italic " + ChatColor.WHITE + "- Курсив (Лидер)");
        player.sendMessage(ChatColor.YELLOW + "/team underline " + ChatColor.WHITE + "- Подчёркнутый (Лидер)");
        player.sendMessage(ChatColor.YELLOW + "/team reset " + ChatColor.WHITE + "- Обычный (Лидер)");
        player.sendMessage(ChatColor.YELLOW + "/team prefix <prefix> " + ChatColor.WHITE + "- Позывной");
        player.sendMessage(ChatColor.YELLOW + "/myteam " + ChatColor.WHITE + "- Меню команды");
        player.sendMessage(ChatColor.YELLOW + "/tc <message> " + ChatColor.WHITE + "- Командный чат");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team create <название>");
            return;
        }
        String teamName = args[1];
        if (teamManager.getTeam(teamName) != null) {
            player.sendMessage(ChatColor.RED + "Команда с таким названием уже существует!");
            return;
        }
        if (teamManager.getPlayerTeam(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Ты уже в команде!");
            return;
        }
        Team team = teamManager.createTeam(teamName, player.getUniqueId());
        if (team != null) {
            player.sendMessage(ChatColor.GREEN + "Команда '" + teamName + "' создана!");
            player.sendMessage(ChatColor.GRAY + "Ты лидер. Используй /team invite для приглашения.");
            teamManager.updatePlayerTab(player);
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось создать команду.");
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team invite <игрок>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер может приглашать!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        if (teamManager.getPlayerTeam(target.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Этот игрок уже в команде!");
            return;
        }
        teamManager.addInvitation(target.getUniqueId(), team.getName());
        target.sendMessage(ChatColor.GOLD + "Тебя пригласили в команду '" +
                team.getColoredName() + ChatColor.GOLD + "' от " + player.getName());
        target.sendMessage(ChatColor.YELLOW + "Введи " + ChatColor.GREEN + "/team accept" +
                ChatColor.YELLOW + " или " + ChatColor.RED + "/team decline");
        player.sendMessage(ChatColor.GREEN + "Приглашение отправлено " + target.getName() + "!");
    }

    private void handleAccept(Player player) {
        String teamName = teamManager.getInvitation(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatColor.RED + "У тебя нет приглашений!");
            return;
        }
        Team team = teamManager.getTeam(teamName);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Эта команда больше не существует!");
            teamManager.removeInvitation(player.getUniqueId());
            return;
        }
        if (teamManager.addPlayerToTeam(teamName, player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Ты присоединился к команде '" + team.getColoredName() + ChatColor.GREEN + "'!");
            for (UUID member : team.getMembers()) {
                Player p = Bukkit.getPlayer(member);
                if (p != null && !p.equals(player)) {
                    p.sendMessage(ChatColor.YELLOW + player.getName() + " присоединился!");
                }
            }
            teamManager.removeInvitation(player.getUniqueId());
            teamManager.updateAllTeamTab(team);
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось присоединиться!");
        }
    }

    private void handleDecline(Player player) {
        String teamName = teamManager.getInvitation(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatColor.RED + "У тебя нет приглашений!");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "Ты отклонил приглашение в '" + teamName + "'");
        teamManager.removeInvitation(player.getUniqueId());
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team kick <игрок>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер может исключать!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "Ты не можешь исключить себя! Используй /team leave.");
            return;
        }
        if (teamManager.kickPlayerFromTeam(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + target.getName() + " исключён!");
            target.sendMessage(ChatColor.RED + "Тебя исключили из команды!");
            teamManager.updatePlayerTab(target);
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось исключить игрока.");
        }
    }

    private void handleLeave(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (teamManager.removePlayerFromTeam(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Ты покинул команду.");
            teamManager.updatePlayerTab(player);
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось покинуть команду.");
        }
    }

    private void handleInfo(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "=== Информация о команде ===");
        player.sendMessage(ChatColor.YELLOW + "Название: " + team.getColoredNameWithBrackets());
        player.sendMessage(ChatColor.YELLOW + "Лидер: " + Bukkit.getOfflinePlayer(team.getLeader()).getName());
        player.sendMessage(ChatColor.YELLOW + "Цвет: " + team.getColor() + team.getColor().name());
        player.sendMessage(ChatColor.YELLOW + "Стиль: " + getStyleName(team.getTextFormat()));
        player.sendMessage(ChatColor.YELLOW + "Описание: " + ChatColor.WHITE +
                (team.getDescription().isEmpty() ? "Нет описания" : team.getDescription()));
        player.sendMessage(ChatColor.YELLOW + "Участников: " + team.getMemberCount());
        for (UUID member : team.getMembers()) {
            String name = Bukkit.getOfflinePlayer(member).getName();
            String prefix = team.getPrefix(member);
            String role = member.equals(team.getLeader()) ? ChatColor.GOLD + "Лидер" : ChatColor.GRAY + "Участник";
            player.sendMessage(ChatColor.YELLOW + "  - " + role + ChatColor.WHITE + ": " + name + " " + prefix);
        }
    }

    private void handleDesc(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер может установить описание!");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team desc <описание>");
            return;
        }
        StringBuilder desc = new StringBuilder();
        for (int i = 1; i < args.length; i++) desc.append(args[i]).append(" ");
        team.setDescription(desc.toString().trim());
        teamManager.saveTeams();
        player.sendMessage(ChatColor.GREEN + "Описание обновлено!");
    }

    private void handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team color <цвет>");
            player.sendMessage(ChatColor.YELLOW + "Цвета: red, green, blue, yellow, gold, aqua, dark_red, dark_green, dark_blue, dark_purple, black, white, gray");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер может установить цвет!");
            return;
        }
        ChatColor color;
        try {
            color = ChatColor.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Неверный цвет!");
            return;
        }
        team.setColor(color);
        teamManager.saveTeams();
        teamManager.updateAllTeamTab(team);
        player.sendMessage(ChatColor.GREEN + "Цвет изменён на " + color + color.name());
    }

    private void handlePrefix(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team prefix <позывной>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        StringBuilder pref = new StringBuilder();
        for (int i = 1; i < args.length; i++) pref.append(args[i]).append(" ");
        String prefix = pref.toString().trim();
        team.setPrefix(player.getUniqueId(), prefix);
        teamManager.saveTeams();
        teamManager.updatePlayerTab(player);
        player.sendMessage(ChatColor.GREEN + "Позывной установлен: " + ChatColor.WHITE + prefix);
    }
}