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
    private final EventGUI eventGUI;
    private final ZoneGUI zoneGUI;
    private final AdminHubGUI adminHubGUI;

    public CommandHandler(IventWar plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.eventGUI = new EventGUI(plugin);
        this.zoneGUI = new ZoneGUI(plugin);
        this.adminHubGUI = new AdminHubGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Только игроки могут использовать команды!");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        // ---------- Секретная команда ----------
        if (cmd.equals("topbro")) {
            handleTopBro(player);
            return true;
        }

        // ---------- Командный чат ----------
        if (cmd.equals("tc")) {
            handleTeamChat(player, args);
            return true;
        }

        // ---------- Меню команды ----------
        if (cmd.equals("myteam") || (cmd.equals("my") && args.length > 0 && args[0].equalsIgnoreCase("team"))) {
            handleMyTeam(player);
            return true;
        }

        // ---------- Команды для OP ----------
        if (cmd.equals("startevent") || cmd.equals("startivent")) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            eventGUI.openMenu(player);
            return true;
        }

        if (cmd.equals("closeevent")) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Использование: /closeEvent <причина>");
                return true;
            }
            StringBuilder reason = new StringBuilder();
            for (String arg : args) reason.append(arg).append(" ");
            String reasonText = reason.toString().trim();
            if (!plugin.getEventManager().isEventActive()) {
                player.sendMessage(ChatColor.RED + "Ивент не активен!");
                return true;
            }
            plugin.getEventManager().forceCloseEvent(reasonText);
            player.sendMessage(ChatColor.GREEN + "Ивент завершён с причиной: " + reasonText);
            return true;
        }

        if (cmd.equals("createzone")) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            zoneGUI.openMenu(player);
            return true;
        }

        if (cmd.equals("zone")) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            zoneGUI.openZoneList(player);
            return true;
        }

        if (cmd.equals("adminhub")) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            adminHubGUI.openMenu(player);
            return true;
        }

        // ---------- Основная команда /team ----------
        if (cmd.equals("team")) {
            if (args.length == 0) {
                sendHelp(player);
                return true;
            }
            String subCmd = args[0].toLowerCase();
            switch (subCmd) {
                case "create":   handleCreate(player, args); break;
                case "invite":   handleInvite(player, args); break;
                case "accept":   handleAccept(player); break;
                case "decline":  handleDecline(player); break;
                case "kick":     handleKick(player, args); break;
                case "leave":    handleLeave(player); break;
                case "info":     handleInfo(player); break;
                case "desc":     handleDesc(player, args); break;
                case "color":    handleColor(player, args); break;
                case "prefix":   handlePrefix(player, args); break;
                case "transfer": handleTransfer(player, args); break;
                case "promote":  handlePromote(player, args); break;
                case "demote":   handleDemote(player, args); break;
                default:         sendHelp(player); break;
            }
        }
        return true;
    }

    // ========================================================================
    // ОБРАБОТЧИКИ КОМАНД
    // ========================================================================

    // ---------- /topbro ----------
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

    // ---------- /tc ----------
    private void handleTeamChat(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Использование: /tc <сообщение>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        StringBuilder message = new StringBuilder();
        for (String arg : args) message.append(arg).append(" ");
        String msg = message.toString().trim();
        String formatted = ChatColor.GOLD + "[Team] " + ChatColor.YELLOW + player.getName() +
                ChatColor.GRAY + ": " + ChatColor.WHITE + msg;
        for (UUID member : team.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) p.sendMessage(formatted);
        }
    }

    // ---------- /myteam ----------
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
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Описание: " + ChatColor.WHITE +
                (team.getDescription().isEmpty() ? "Нет описания" : team.getDescription()));
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Участников: " + ChatColor.WHITE + team.getMemberCount());
        player.sendMessage(ChatColor.GOLD + "╠═══════════════════════════════════╣");
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Участники:");
        for (UUID member : team.getMembers()) {
            String name = Bukkit.getOfflinePlayer(member).getName();
            String prefix = team.getPrefix(member);
            String role = team.getRole(member);
            String roleDisplay;
            if (role.equals("leader")) roleDisplay = ChatColor.GOLD + "★ Лидер";
            else if (role.equals("helper")) roleDisplay = ChatColor.AQUA + "✪ Помощник";
            else roleDisplay = ChatColor.GRAY + "• Участник";
            player.sendMessage(ChatColor.GOLD + "║   " + roleDisplay + ChatColor.WHITE + ": " + name + " " + prefix);
        }
        player.sendMessage(ChatColor.GOLD + "╚═══════════════════════════════════╝");
        if (team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Ты лидер!");
        } else if (team.isHelper(player.getUniqueId())) {
            player.sendMessage(ChatColor.AQUA + "Ты помощник!");
        }
    }

    // ---------- /team create ----------
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team create <название>");
            return;
        }
        String teamName = args[1];
        if (teamManager.getTeam(teamName) != null) {
            player.sendMessage(ChatColor.RED + "Команда уже существует!");
            return;
        }
        if (teamManager.getPlayerTeam(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Ты уже в команде!");
            return;
        }
        Team team = teamManager.createTeam(teamName, player.getUniqueId());
        if (team != null) {
            player.sendMessage(ChatColor.GREEN + "Команда '" + teamName + "' создана!");
            teamManager.updatePlayerDisplay(player);
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось создать команду.");
        }
    }

    // ---------- /team invite ----------
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
        if (!team.isLeader(player.getUniqueId()) && !team.isHelper(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер или помощник могут приглашать!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        if (teamManager.getPlayerTeam(target.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Игрок уже в команде!");
            return;
        }
        teamManager.addInvitation(target.getUniqueId(), team.getName());
        target.sendMessage(ChatColor.GOLD + "Тебя пригласили в команду '" + team.getColoredName() + ChatColor.GOLD + "' от " + player.getName());
        target.sendMessage(ChatColor.YELLOW + "Введи " + ChatColor.GREEN + "/team accept" + ChatColor.YELLOW + " или " + ChatColor.RED + "/team decline");
        player.sendMessage(ChatColor.GREEN + "Приглашение отправлено " + target.getName() + "!");
    }

    // ---------- /team accept ----------
    private void handleAccept(Player player) {
        String teamName = teamManager.getInvitation(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatColor.RED + "У тебя нет приглашений!");
            return;
        }
        Team team = teamManager.getTeam(teamName);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Команда не существует!");
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

    // ---------- /team decline ----------
    private void handleDecline(Player player) {
        String teamName = teamManager.getInvitation(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatColor.RED + "У тебя нет приглашений!");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "Ты отклонил приглашение в '" + teamName + "'");
        teamManager.removeInvitation(player.getUniqueId());
    }

    // ---------- /team kick ----------
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
            player.sendMessage(ChatColor.RED + "Нельзя исключить себя! Используй /team leave.");
            return;
        }
        if (!team.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Игрок не в твоей команде!");
            return;
        }
        if (teamManager.kickPlayerFromTeam(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + target.getName() + " исключён!");
            target.sendMessage(ChatColor.RED + "Тебя исключили из команды!");
            teamManager.updatePlayerDisplay(target);
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось исключить игрока.");
        }
    }

    // ---------- /team leave ----------
    private void handleLeave(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (teamManager.removePlayerFromTeam(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Ты покинул команду.");
            teamManager.updatePlayerDisplay(player);
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось покинуть команду.");
        }
    }

    // ---------- /team info ----------
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
        player.sendMessage(ChatColor.YELLOW + "Описание: " + ChatColor.WHITE +
                (team.getDescription().isEmpty() ? "Нет описания" : team.getDescription()));
        player.sendMessage(ChatColor.YELLOW + "Участников: " + team.getMemberCount());
        for (UUID member : team.getMembers()) {
            String name = Bukkit.getOfflinePlayer(member).getName();
            String prefix = team.getPrefix(member);
            String role = team.getRole(member);
            String roleDisplay;
            if (role.equals("leader")) roleDisplay = ChatColor.GOLD + "Лидер";
            else if (role.equals("helper")) roleDisplay = ChatColor.AQUA + "Помощник";
            else roleDisplay = ChatColor.GRAY + "Участник";
            player.sendMessage(ChatColor.YELLOW + "  - " + roleDisplay + ChatColor.WHITE + ": " + name + " " + prefix);
        }
    }

    // ---------- /team desc ----------
    private void handleDesc(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId()) && !team.isHelper(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер или помощник могут менять описание!");
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

    // ---------- /team color ----------
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
        if (!team.isLeader(player.getUniqueId()) && !team.isHelper(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер или помощник могут менять цвет!");
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

    // ---------- /team prefix ----------
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
        teamManager.updatePlayerDisplay(player);
        player.sendMessage(ChatColor.GREEN + "Позывной установлен: " + ChatColor.WHITE + prefix);
    }

    // ---------- /team transfer ----------
    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team transfer <игрок>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер может передавать лидерство!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Нельзя передать лидерство самому себе!");
            return;
        }
        if (!team.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Игрок не в твоей команде!");
            return;
        }
        if (teamManager.transferLeadership(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Лидерство передано " + target.getName());
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось передать лидерство.");
        }
    }

    // ---------- /team promote ----------
    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team promote <игрок>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер может назначать помощников!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Нельзя сделать себя помощником!");
            return;
        }
        if (!team.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Игрок не в твоей команде!");
            return;
        }
        if (team.isLeader(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Лидер не может быть помощником!");
            return;
        }
        if (teamManager.promoteToHelper(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + target.getName() + " теперь помощник!");
            target.sendMessage(ChatColor.AQUA + "Теперь ты помощник в команде '" + team.getColoredName() + ChatColor.AQUA + "'!");
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось назначить помощника.");
        }
    }

    // ---------- /team demote ----------
    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /team demote <игрок>");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Ты не в команде!");
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только лидер может понижать!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Нельзя понизить себя!");
            return;
        }
        if (!team.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Игрок не в твоей команде!");
            return;
        }
        if (team.isLeader(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Лидера нельзя понизить!");
            return;
        }
        if (teamManager.demoteToMember(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + target.getName() + " теперь участник.");
            target.sendMessage(ChatColor.YELLOW + "Тебя понизили до участника в команде '" + team.getColoredName() + ChatColor.YELLOW + "'.");
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось понизить.");
        }
    }

    // ---------- /help (компактная) ----------
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== IventWar Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/team <create|invite|accept|decline|kick|leave|info|desc|color|prefix|transfer|promote|demote>");
        player.sendMessage(ChatColor.YELLOW + "/myteam " + ChatColor.WHITE + "- Меню команды");
        player.sendMessage(ChatColor.YELLOW + "/tc <msg> " + ChatColor.WHITE + "- Командный чат");
        player.sendMessage(ChatColor.YELLOW + "/startevent " + ChatColor.WHITE + "- Настройка ивента (OP)");
        player.sendMessage(ChatColor.YELLOW + "/closeEvent <reason> " + ChatColor.WHITE + "- Завершить ивент (OP)");
        player.sendMessage(ChatColor.YELLOW + "/createzone " + ChatColor.WHITE + "- Создать зону (OP)");
        player.sendMessage(ChatColor.YELLOW + "/zone " + ChatColor.WHITE + "- Список зон (OP)");
        player.sendMessage(ChatColor.YELLOW + "/adminhub " + ChatColor.WHITE + "- Админ-панель (OP)");
    }
}