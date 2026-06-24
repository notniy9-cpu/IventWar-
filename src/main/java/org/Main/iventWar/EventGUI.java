package org.Main.iventWar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EventGUI implements Listener {
    private final IventWar plugin;
    private final EventManager eventManager;
    private final TeamManager teamManager;
    private Player waitingForInput;
    private enum InputType { NONE, DURATION, COORDINATES }
    private InputType currentInputType = InputType.NONE;
    private String currentTeamForCoords; // для какой команды задаём координаты

    public EventGUI(IventWar plugin) {
        this.plugin = plugin;
        this.eventManager = plugin.getEventManager();
        this.teamManager = plugin.getTeamManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lНАСТРОЙКА ИВЕНТА");

        // 1. Выбор команд
        String teamsDisplay = getTeamsDisplay();
        ItemStack teamItem = createItem(Material.NAME_TAG,
                "§aВыбор команд",
                "§7Выбрано: " + teamsDisplay,
                "§eНажмите, чтобы выбрать команды");
        inv.setItem(10, teamItem);

        // 2. Длительность
        ItemStack durationItem = createItem(Material.CLOCK,
                "§6Длительность",
                "§7Текущая: " + getDurationDisplay(),
                "§eНажмите, чтобы изменить");
        inv.setItem(19, durationItem);

        // 3. Погода
        ItemStack weatherItem = createItem(Material.WATER_BUCKET,
                "§bПогода",
                "§7Текущая: " + getWeatherDisplay(),
                "§eНажмите, чтобы изменить");
        inv.setItem(28, weatherItem);

        // 4. Время суток
        ItemStack timeItem = createItem(Material.CLOCK,
                "§dВремя суток",
                "§7Текущее: " + getTimeDisplay(),
                "§eНажмите, чтобы изменить");
        inv.setItem(37, timeItem);

        // 5. Координаты команд (НОВОЕ)
        ItemStack spawnItem = createItem(Material.COMPASS,
                "§6Координаты команд",
                "§7Настроено для " + getTeamSpawnCount() + " команд",
                "§eНажмите, чтобы настроить координаты",
                "§7(для каждой команды отдельно)");
        inv.setItem(46, spawnItem);

        // 6. Кнопка СТАРТ
        ItemStack startItem = createItem(Material.EMERALD_BLOCK,
                "§a§l▶ ЗАПУСТИТЬ ИВЕНТ!",
                "§7Все настройки будут применены",
                "§eНажмите для запуска!");
        inv.setItem(49, startItem);

        player.openInventory(inv);
    }

    private String getTeamsDisplay() {
        List<String> teams = eventManager.getSelectedTeams();
        if (teams.isEmpty()) return "§cне выбраны";
        return String.join(", ", teams);
    }

    private String getDurationDisplay() {
        int min = eventManager.getEventDuration();
        if (min <= 0) return "§cНе установлено";
        return "§6" + min + " мин";
    }

    private String getWeatherDisplay() {
        String w = eventManager.getWeather();
        switch (w) {
            case "clear": return "§eЯсно";
            case "rain": return "§bДождь";
            case "thunder": return "§cГроза";
            default: return "§7Неизвестно";
        }
    }

    private String getTimeDisplay() {
        String t = eventManager.getTimeOfDay();
        switch (t) {
            case "day": return "§eДень";
            case "night": return "§9Ночь";
            case "sunset": return "§6Закат";
            case "sunrise": return "§dРассвет";
            case "keep": return "§7Сохранить текущее";
            default: return "§7Неизвестно";
        }
    }

    private String getTeamSpawnCount() {
        int count = eventManager.getTeamSpawnLocations().size();
        return count + " команд";
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    // Обработка кликов в главном меню
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lНАСТРОЙКА ИВЕНТА")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        int slot = event.getRawSlot();

        switch (slot) {
            case 10: openTeamSelection(player); break;
            case 19: openDurationSelection(player); break;
            case 28: openWeatherSelection(player); break;
            case 37: openTimeSelection(player); break;
            case 46: openSpawnSettings(player); break;
            case 49:
                if (eventManager.getEventDuration() <= 0) {
                    player.sendMessage("§cУстановите длительность ивента!");
                    return;
                }
                if (eventManager.getSelectedTeams().isEmpty()) {
                    player.sendMessage("§cВыберите хотя бы одну команду!");
                    return;
                }
                eventManager.startEvent();
                player.closeInventory();
                player.sendMessage("§aИвент запущен!");
                break;
        }
    }

    // ---------- Подменю выбора команд (множественный выбор) ----------
    private void openTeamSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§aВыбор команд");
        ItemStack allItem = createItem(Material.GRASS_BLOCK, "§aВсе игроки",
                "§7Все онлайн игроки",
                "§eНажмите, чтобы выбрать/отменить");
        inv.setItem(0, allItem);

        ItemStack doneItem = createItem(Material.LIME_WOOL, "§a§lГОТОВО",
                "§7Сохранить выбранные команды");
        inv.setItem(53, doneItem);

        int slot = 10;
        for (Team team : teamManager.getAllTeams()) {
            if (slot >= 53) break;
            String teamName = team.getName();
            boolean selected = eventManager.getSelectedTeams().contains(teamName);
            Material material = selected ? Material.GREEN_WOOL : Material.WHITE_WOOL;
            String status = selected ? "§a✓ Выбрана" : "§7✗ Не выбрана";
            ItemStack teamItem = createItem(material,
                    "§6" + teamName,
                    "§7Участников: " + team.getMemberCount(),
                    status,
                    "§eНажмите, чтобы переключить");
            inv.setItem(slot, teamItem);
            slot++;
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onTeamSelectionClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§aВыбор команд")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        int slot = event.getRawSlot();

        if (display.equals("ГОТОВО")) {
            player.closeInventory();
            openMenu(player);
            return;
        }

        if (display.equals("Все игроки")) {
            List<String> teams = eventManager.getSelectedTeams();
            if (teams.contains("all")) {
                teams.remove("all");
                player.sendMessage("§aВсе игроки отключены");
            } else {
                teams.add("all");
                teams.removeIf(t -> !t.equals("all"));
                player.sendMessage("§aВыбраны все игроки (другие команды сброшены)");
            }
            openTeamSelection(player);
            return;
        }

        String teamName = display;
        Team team = teamManager.getTeam(teamName);
        if (team == null) return;

        List<String> selected = eventManager.getSelectedTeams();
        if (selected.contains(teamName)) {
            selected.remove(teamName);
            player.sendMessage("§cКоманда '" + teamName + "' исключена");
        } else {
            selected.add(teamName);
            selected.remove("all");
            player.sendMessage("§aКоманда '" + teamName + "' добавлена");
        }
        openTeamSelection(player);
    }

    // ---------- Подменю настроек координат команд ----------
    private void openSpawnSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6Координаты команд");

        // Кнопка сброса всех координат
        ItemStack resetAll = createItem(Material.BARRIER, "§cСбросить все координаты",
                "§7Удалить координаты для всех команд");
        inv.setItem(0, resetAll);

        // Кнопка "Назад"
        ItemStack back = createItem(Material.ARROW, "§eНазад", "§7Вернуться в главное меню");
        inv.setItem(53, back);

        int slot = 10;
        for (String teamName : eventManager.getSelectedTeams()) {
            if (teamName.equals("all")) continue; // пропускаем "все игроки"
            if (slot >= 52) break;
            Location loc = eventManager.getTeamSpawnLocations().get(teamName);
            String coords = (loc == null) ? "§cне указаны" : "§6" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
            ItemStack item = createItem(Material.COMPASS,
                    "§6" + teamName,
                    "§7Координаты: " + coords,
                    "§eНажмите, чтобы установить",
                    "§7(введите в чат: x y z)");
            inv.setItem(slot, item);
            slot++;
        }

        // Если нет команд, показываем сообщение
        if (slot == 10) {
            ItemStack empty = createItem(Material.RED_STAINED_GLASS_PANE, "§cНет выбранных команд",
                    "§7Сначала выберите команды в главном меню");
            inv.setItem(22, empty);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onSpawnSettingsClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6Координаты команд")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        int slot = event.getRawSlot();

        if (display.equals("Назад")) {
            player.closeInventory();
            openMenu(player);
            return;
        }

        if (display.equals("Сбросить все координаты")) {
            eventManager.clearAllSpawnLocations();
            player.sendMessage("§aВсе координаты сброшены!");
            openSpawnSettings(player);
            return;
        }

        // Проверяем, является ли предмет командой
        String teamName = display;
        Team team = teamManager.getTeam(teamName);
        if (team != null) {
            player.closeInventory();
            currentInputType = InputType.COORDINATES;
            currentTeamForCoords = teamName;
            waitingForInput = player;
            player.sendMessage("§eВведите координаты для команды §6" + teamName + " §eчерез пробел: §6x y z");
            player.sendMessage("§7(например: 100 64 200) или напишите §cотмена");
        }
    }

    // ---------- Остальные подменю (длительность, погода, время) ----------
    private void openDurationSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Длительность");
        inv.setItem(10, createItem(Material.BOOK, "§eКастомное", "§7Введите в чат минуты"));
        inv.setItem(13, createItem(Material.COOKIE, "§e30 минут"));
        inv.setItem(16, createItem(Material.CAKE, "§e1 час (60 мин)"));
        player.openInventory(inv);
    }

    private void openWeatherSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§bПогода");
        inv.setItem(10, createItem(Material.SUNFLOWER, "§eЯсно"));
        inv.setItem(13, createItem(Material.WATER_BUCKET, "§bДождь"));
        inv.setItem(16, createItem(Material.LAVA_BUCKET, "§cГроза"));
        player.openInventory(inv);
    }

    private void openTimeSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§dВремя суток");
        inv.setItem(10, createItem(Material.SUNFLOWER, "§eДень"));
        inv.setItem(11, createItem(Material.TORCH, "§9Ночь"));
        inv.setItem(14, createItem(Material.ORANGE_DYE, "§6Закат"));
        inv.setItem(15, createItem(Material.YELLOW_DYE, "§dРассвет"));
        inv.setItem(16, createItem(Material.CLOCK, "§7Сохранить текущее"));
        player.openInventory(inv);
    }

    // Обработка кликов в подменю (длительность, погода, время)
    @EventHandler
    public void onSubMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("§6Длительность")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            if (display.contains("Кастомное")) {
                player.closeInventory();
                currentInputType = InputType.DURATION;
                waitingForInput = player;
                player.sendMessage("§eВведите количество минут в чат (число):");
            } else if (display.contains("30 минут")) {
                eventManager.setEventDuration(30);
                player.sendMessage("§aДлительность установлена: 30 минут");
                player.closeInventory();
                openMenu(player);
            } else if (display.contains("1 час")) {
                eventManager.setEventDuration(60);
                player.sendMessage("§aДлительность установлена: 1 час");
                player.closeInventory();
                openMenu(player);
            }
        }
        else if (title.equals("§bПогода")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            switch (display) {
                case "Ясно": eventManager.setWeather("clear"); break;
                case "Дождь": eventManager.setWeather("rain"); break;
                case "Гроза": eventManager.setWeather("thunder"); break;
                default: return;
            }
            player.sendMessage("§aПогода установлена: " + display);
            player.closeInventory();
            openMenu(player);
        }
        else if (title.equals("§dВремя суток")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            switch (display) {
                case "День": eventManager.setTimeOfDay("day"); break;
                case "Ночь": eventManager.setTimeOfDay("night"); break;
                case "Закат": eventManager.setTimeOfDay("sunset"); break;
                case "Рассвет": eventManager.setTimeOfDay("sunrise"); break;
                case "Сохранить текущее": eventManager.setTimeOfDay("keep"); break;
                default: return;
            }
            player.sendMessage("§aВремя суток установлено: " + display);
            player.closeInventory();
            openMenu(player);
        }
    }

    // Обработка ввода в чат (длительность или координаты)
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (waitingForInput == null) return;
        if (!event.getPlayer().equals(waitingForInput)) return;
        event.setCancelled(true);

        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("отмена")) {
            waitingForInput.sendMessage("§cВвод отменён");
            waitingForInput = null;
            currentInputType = InputType.NONE;
            currentTeamForCoords = null;
            openMenu(event.getPlayer());
            return;
        }

        if (currentInputType == InputType.DURATION) {
            try {
                int minutes = Integer.parseInt(msg);
                if (minutes <= 0 || minutes > 1440) {
                    waitingForInput.sendMessage("§cВведите число от 1 до 1440 (24 часа)");
                    return;
                }
                eventManager.setEventDuration(minutes);
                waitingForInput.sendMessage("§aДлительность установлена: " + minutes + " минут");
                waitingForInput = null;
                currentInputType = InputType.NONE;
                openMenu(event.getPlayer());
            } catch (NumberFormatException e) {
                waitingForInput.sendMessage("§cВведите корректное число!");
            }
        }
        else if (currentInputType == InputType.COORDINATES) {
            String[] parts = msg.split(" ");
            if (parts.length != 3) {
                waitingForInput.sendMessage("§cВведите три числа через пробел: x y z");
                return;
            }
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                Location loc = new Location(waitingForInput.getWorld(), x, y, z);
                eventManager.setTeamSpawnLocation(currentTeamForCoords, loc);
                waitingForInput.sendMessage("§aКоординаты для команды '" + currentTeamForCoords + "' установлены: " +
                        loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
                waitingForInput = null;
                currentInputType = InputType.NONE;
                currentTeamForCoords = null;
                openMenu(event.getPlayer());
            } catch (NumberFormatException e) {
                waitingForInput.sendMessage("§cВведите корректные числа!");
            }
        }
    }
}