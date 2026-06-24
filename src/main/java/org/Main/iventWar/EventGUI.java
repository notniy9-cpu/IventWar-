package org.Main.iventWar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class EventGUI implements Listener {
    private final IventWar plugin;
    private final EventManager eventManager;
    private final TeamManager teamManager;
    private Player waitingForInput;

    public EventGUI(IventWar plugin) {
        this.plugin = plugin;
        this.eventManager = plugin.getEventManager();
        this.teamManager = plugin.getTeamManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Открыть главное меню
    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lНАСТРОЙКА ИВЕНТА");

        // 1. Выбор команды (слот 10-16)
        ItemStack teamItem = createItem(Material.NAME_TAG,
                "§aВыбор команды",
                "§7Текущая: " + getTeamDisplay(),
                "§eНажмите, чтобы выбрать команду",
                "§7(или §aВсе игроки§7)");
        inv.setItem(10, teamItem);

        // 2. Длительность (слот 19-25)
        ItemStack durationItem = createItem(Material.CLOCK,
                "§6Длительность",
                "§7Текущая: " + getDurationDisplay(),
                "§eНажмите, чтобы изменить");
        inv.setItem(19, durationItem);

        // 3. Погода (слот 28-34)
        ItemStack weatherItem = createItem(Material.WATER_BUCKET,
                "§bПогода",
                "§7Текущая: " + getWeatherDisplay(),
                "§eНажмите, чтобы изменить");
        inv.setItem(28, weatherItem);

        // 4. Время суток (слот 37-43)
        ItemStack timeItem = createItem(Material.CLOCK,
                "§dВремя суток",
                "§7Текущее: " + getTimeDisplay(),
                "§eНажмите, чтобы изменить");
        inv.setItem(37, timeItem);

        // 5. Кнопка СТАРТ (слот 49)
        ItemStack startItem = createItem(Material.EMERALD_BLOCK,
                "§a§l▶ ЗАПУСТИТЬ ИВЕНТ!",
                "§7Все настройки будут применены",
                "§eНажмите для запуска!");
        inv.setItem(49, startItem);

        player.openInventory(inv);
    }

    private String getTeamDisplay() {
        String team = eventManager.getSelectedTeam();
        if (team.equals("all")) return "§aВсе игроки";
        return "§6" + team;
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
            case 49:
                if (eventManager.getEventDuration() <= 0) {
                    player.sendMessage("§cУстановите длительность ивента!");
                    return;
                }
                eventManager.startEvent();
                player.closeInventory();
                player.sendMessage("§aИвент запущен!");
                break;
        }
    }

    // Подменю выбора команды
    private void openTeamSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§aВыбор команды");
        inv.setItem(13, createItem(Material.GRASS_BLOCK, "§aВсе игроки", "§7Все онлайн игроки"));

        int slot = 10;
        for (Team team : teamManager.getAllTeams()) {
            if (slot > 16) break;
            // ИСПРАВЛЕНО: вместо BANNER используем WHITE_BANNER
            ItemStack teamItem = createItem(Material.WHITE_BANNER,
                    "§6" + team.getName(),
                    "§7Участников: " + team.getMemberCount(),
                    "§eНажмите, чтобы выбрать");
            inv.setItem(slot, teamItem);
            slot++;
        }
        player.openInventory(inv);
    }

    // Подменю длительности
    private void openDurationSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Длительность");
        inv.setItem(10, createItem(Material.BOOK, "§eКастомное", "§7Введите в чат минуты"));
        inv.setItem(13, createItem(Material.COOKIE, "§e30 минут"));
        inv.setItem(16, createItem(Material.CAKE, "§e1 час (60 мин)"));
        player.openInventory(inv);
    }

    // Подменю погоды
    private void openWeatherSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§bПогода");
        inv.setItem(10, createItem(Material.SUNFLOWER, "§eЯсно"));
        inv.setItem(13, createItem(Material.WATER_BUCKET, "§bДождь"));
        inv.setItem(16, createItem(Material.LAVA_BUCKET, "§cГроза"));
        player.openInventory(inv);
    }

    // Подменю времени суток
    private void openTimeSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§dВремя суток");
        inv.setItem(10, createItem(Material.SUNFLOWER, "§eДень"));
        inv.setItem(11, createItem(Material.TORCH, "§9Ночь"));
        inv.setItem(14, createItem(Material.ORANGE_DYE, "§6Закат"));
        inv.setItem(15, createItem(Material.YELLOW_DYE, "§dРассвет"));
        inv.setItem(16, createItem(Material.CLOCK, "§7Сохранить текущее"));
        player.openInventory(inv);
    }

    // Обработка кликов в подменю
    @EventHandler
    public void onSubMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("§aВыбор команды")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            if (display.contains("Все игроки")) {
                eventManager.setSelectedTeam("all");
            } else {
                String teamName = display;
                if (teamManager.getTeam(teamName) != null) {
                    eventManager.setSelectedTeam(teamName);
                } else {
                    player.sendMessage("§cКоманда не найдена!");
                    return;
                }
            }
            player.sendMessage("§aВыбрана команда: " + eventManager.getSelectedTeam());
            player.closeInventory();
            openMenu(player);
        }
        else if (title.equals("§6Длительность")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            if (display.contains("Кастомное")) {
                player.closeInventory();
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

    // Обработка ввода кастомной длительности
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (waitingForInput == null) return;
        if (!event.getPlayer().equals(waitingForInput)) return;
        event.setCancelled(true);

        try {
            int minutes = Integer.parseInt(event.getMessage().trim());
            if (minutes <= 0 || minutes > 1440) {
                waitingForInput.sendMessage("§cВведите число от 1 до 1440 (24 часа)");
                return;
            }
            eventManager.setEventDuration(minutes);
            waitingForInput.sendMessage("§aДлительность установлена: " + minutes + " минут");
            waitingForInput = null;
            openMenu(event.getPlayer());
        } catch (NumberFormatException e) {
            waitingForInput.sendMessage("§cВведите корректное число!");
        }
    }
}