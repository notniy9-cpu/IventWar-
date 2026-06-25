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
    private enum InputType { NONE, DURATION, COORDINATES, END_SPAWN, EVENT_DELAY, EVENT_RADIUS, EVENT_COUNT, EVENT_INTERVAL, LOOT_CHANCE }
    private InputType currentInputType = InputType.NONE;
    private String currentTeamForCoords;
    private EventManager.EventType editingEvent;

    private ItemStack tempLootItem;

    public EventGUI(IventWar plugin) {
        this.plugin = plugin;
        this.eventManager = plugin.getEventManager();
        this.teamManager = plugin.getTeamManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ========================================================================
    // ГЛАВНОЕ МЕНЮ
    // ========================================================================
    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lНАСТРОЙКА ИВЕНТА");

        inv.setItem(10, createItem(Material.NAME_TAG, "§aВыбор команд",
                "§7Выбрано: " + getTeamsDisplay(),
                "§eЛКМ – добавить | §cПКМ – убрать"));

        inv.setItem(19, createItem(Material.CLOCK, "§6Длительность ивента",
                "§7Текущая: " + getDurationDisplay(),
                "§eНажмите, чтобы изменить"));

        inv.setItem(28, createItem(Material.WATER_BUCKET, "§bПогода",
                "§7Текущая: " + getWeatherDisplay(),
                "§eНажмите, чтобы изменить"));

        inv.setItem(37, createItem(Material.CLOCK, "§dВремя суток",
                "§7Текущее: " + getTimeDisplay(),
                "§eНажмите, чтобы изменить"));

        String cycleStatus = eventManager.isCycleEnabled() ? "§aВключен" : "§cВыключен";
        inv.setItem(40, createItem(Material.REPEATER, "§eЦикл событий",
                "§7Статус: " + cycleStatus,
                "§7Интервал: " + eventManager.getCycleInterval() + " мин",
                "§eНажмите для настройки"));

        inv.setItem(46, createItem(Material.COMPASS, "§6Координаты команд",
                "§7Настроено для " + getTeamSpawnCount() + " команд",
                "§eНажмите, чтобы настроить координаты"));

        inv.setItem(47, createItem(Material.ENDER_PEARL, "§6Спавн после ивента",
                "§7Текущий: " + getEndSpawnDisplay(),
                "§eНажмите, чтобы установить координаты"));

        inv.setItem(48, createItem(Material.NETHER_STAR, "§d§lСОБЫТИЯ",
                "§7Настройка событий: Аэродроп, Бомбардировка, Ядерная",
                "§eНажмите, чтобы настроить события"));

        inv.setItem(49, createItem(Material.EMERALD_BLOCK, "§a§l▶ ЗАПУСТИТЬ ИВЕНТ!",
                "§7Все настройки будут применены",
                "§eНажмите для запуска!"));

        player.openInventory(inv);
    }

    // ----- Меню цикла -----
    public void openCycleMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§eНастройка цикла");
        String status = eventManager.isCycleEnabled() ? "§aВключен" : "§cВыключен";
        inv.setItem(10, createItem(Material.LEVER, "§aВключить/Выключить",
                "§7Текущий статус: " + status,
                "§eНажмите для переключения"));
        inv.setItem(13, createItem(Material.CLOCK, "§6Интервал (минуты)",
                "§7Текущий: " + eventManager.getCycleInterval() + " мин",
                "§eНажмите, чтобы изменить"));
        inv.setItem(16, createItem(Material.ARROW, "§eНазад",
                "§7Вернуться в главное меню"));
        player.openInventory(inv);
    }

    // ----- Меню событий -----
    public void openEventsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§d§lНАСТРОЙКА СОБЫТИЙ");

        EventManager.EventSettings airdrop = eventManager.getSettings(EventManager.EventType.AIRDROP);
        String airdropStatus = airdrop.isEnabled() ? "§aВключено" : "§cВыключено";
        inv.setItem(10, createItem(Material.CHEST, "§6Аэродроп",
                "§7Статус: " + airdropStatus,
                "§7Задержка: " + airdrop.getDelay() + " сек",
                "§7Радиус: " + airdrop.getRadius() + " блоков",
                "§eНажмите для настройки"));

        EventManager.EventSettings bomb = eventManager.getSettings(EventManager.EventType.BOMBARDMENT);
        String bombStatus = bomb.isEnabled() ? "§aВключено" : "§cВыключено";
        inv.setItem(13, createItem(Material.TNT, "§cБомбардировка",
                "§7Статус: " + bombStatus,
                "§7Интервал: " + bomb.getTntInterval() + " тиков",
                "§7Количество TNT: " + bomb.getTntCount(),
                "§7Радиус: " + bomb.getRadius() + " блоков",
                "§eНажмите для настройки"));

        EventManager.EventSettings nuclear = eventManager.getSettings(EventManager.EventType.NUCLEAR);
        String nuclearStatus = nuclear.isEnabled() ? "§aВключено" : "§cВыключено";
        inv.setItem(16, createItem(Material.FIREWORK_ROCKET, "§6Ядерная боеголовка",
                "§7Статус: " + nuclearStatus,
                "§7Задержка: " + nuclear.getDelay() + " сек",
                "§7Сила взрыва: " + nuclear.getExplosionPower(),
                "§eНажмите для настройки"));

        inv.setItem(22, createItem(Material.COMPASS, "§6Общие координаты",
                "§7Установить координаты для всех событий",
                "§eНажмите, чтобы ввести x y z"));
        inv.setItem(23, createItem(Material.BARRIER, "§cСбросить координаты",
                "§7Очистить координаты для всех событий"));

        inv.setItem(26, createItem(Material.ARROW, "§eНазад",
                "§7Вернуться в главное меню"));

        player.openInventory(inv);
    }

    // ----- Меню настройки конкретного события -----
    public void openEventSettings(Player player, EventManager.EventType type) {
        EventManager.EventSettings settings = eventManager.getSettings(type);
        String title = "§6Настройка: " + type.name();

        Inventory inv = Bukkit.createInventory(null, 27, title);

        String status = settings.isEnabled() ? "§aВключено" : "§cВыключено";
        inv.setItem(10, createItem(Material.LEVER, "§aВключить/Выключить",
                "§7Текущий статус: " + status,
                "§eНажмите для переключения"));

        inv.setItem(11, createItem(Material.CLOCK, "§6Задержка до появления",
                "§7Текущая: " + settings.getDelay() + " сек",
                "§eНажмите, чтобы изменить"));

        if (type != EventManager.EventType.NUCLEAR) {
            inv.setItem(12, createItem(Material.COMPASS, "§6Радиус появления",
                    "§7Текущий: " + settings.getRadius() + " блоков",
                    "§eНажмите, чтобы изменить"));
        }

        Location center = settings.getCenter();
        String coords = (center == null) ? "§cНе установлены" :
                "§6" + center.getBlockX() + " " + center.getBlockY() + " " + center.getBlockZ();
        inv.setItem(13, createItem(Material.MAP, "§6Координаты центра",
                "§7Текущие: " + coords,
                "§eНажмите, чтобы установить"));

        if (type == EventManager.EventType.AIRDROP) {
            inv.setItem(14, createItem(Material.CHEST, "§eРедактировать лут",
                    "§7Предметов: " + settings.getLootItems().size(),
                    "§eНажмите, чтобы добавить предмет",
                    "§7(возьмите предмет в руку и нажмите)"));
        }

        if (type == EventManager.EventType.BOMBARDMENT) {
            inv.setItem(14, createItem(Material.TNT, "§eКоличество TNT",
                    "§7Текущее: " + settings.getTntCount(),
                    "§eНажмите, чтобы изменить"));
            inv.setItem(15, createItem(Material.CLOCK, "§eИнтервал между TNT (тики)",
                    "§7Текущий: " + settings.getTntInterval() + " тиков",
                    "§eНажмите, чтобы изменить"));
        }

        if (type == EventManager.EventType.NUCLEAR) {
            inv.setItem(16, createItem(Material.TNT, "§6Сила взрыва",
                    "§7Текущая: " + settings.getExplosionPower(),
                    "§eНажмите, чтобы изменить (0.1 – 250.0)"));
        }

        inv.setItem(24, createItem(Material.COMPASS, "§eТекущие координаты",
                "§7Установить координаты центра в ваше текущее положение",
                "§eНажмите, чтобы установить"));

        inv.setItem(22, createItem(Material.EMERALD_BLOCK, "§a§lЗАПУСТИТЬ СЕЙЧАС!",
                "§7Запустить это событие вручную",
                "§eНажмите для запуска"));

        inv.setItem(26, createItem(Material.ARROW, "§eНазад",
                "§7Вернуться к списку событий"));

        player.openInventory(inv);
    }

    // ========================================================================
    // ОБРАБОТЧИК КЛИКОВ
    // ========================================================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Игнорируем клики по инвентарю игрока (нижняя часть)
        if (event.getRawSlot() >= event.getInventory().getSize()) return;

        // ---- Главное меню ----
        if (title.equals("§6§lНАСТРОЙКА ИВЕНТА")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            switch (event.getRawSlot()) {
                case 10: openTeamSelection(player); break;
                case 19: openDurationSelection(player); break;
                case 28: openWeatherSelection(player); break;
                case 37: openTimeSelection(player); break;
                case 40: openCycleMenu(player); break;
                case 46: openSpawnSettings(player); break;
                case 47: promptEndSpawnCoordinates(player); break;
                case 48: openEventsMenu(player); break;
                case 49: startMainEvent(player); break;
            }
            return;
        }

        // ---- Меню цикла ----
        if (title.equals("§eНастройка цикла")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            int slot = event.getRawSlot();
            switch (slot) {
                case 10:
                    eventManager.setCycleEnabled(!eventManager.isCycleEnabled());
                    player.sendMessage("§aЦикл событий " + (eventManager.isCycleEnabled() ? "включен" : "выключен"));
                    openCycleMenu(player);
                    break;
                case 13:
                    player.closeInventory();
                    currentInputType = InputType.EVENT_COUNT;
                    editingEvent = null;
                    waitingForInput = player;
                    player.sendMessage("§eВведите интервал цикла (минуты, от 1 до 60):");
                    break;
                case 16:
                    openMenu(player);
                    break;
            }
            return;
        }

        // ---- Меню событий ----
        if (title.equals("§d§lНАСТРОЙКА СОБЫТИЙ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            int slot = event.getRawSlot();
            switch (slot) {
                case 10: openEventSettings(player, EventManager.EventType.AIRDROP); break;
                case 13: openEventSettings(player, EventManager.EventType.BOMBARDMENT); break;
                case 16: openEventSettings(player, EventManager.EventType.NUCLEAR); break;
                case 22:
                    player.closeInventory();
                    currentInputType = InputType.COORDINATES;
                    editingEvent = null;
                    waitingForInput = player;
                    player.sendMessage("§eВведите координаты через пробел для всех событий: §6x y z");
                    player.sendMessage("§7(или напишите §cотмена§7)");
                    break;
                case 23:
                    eventManager.clearCommonCoordinates();
                    player.sendMessage("§aОбщие координаты сброшены!");
                    openEventsMenu(player);
                    break;
                case 26:
                    openMenu(player);
                    break;
            }
            return;
        }

        // ---- Настройка конкретного события ----
        if (title.startsWith("§6Настройка: ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            String typeName = title.replace("§6Настройка: ", "");
            EventManager.EventType type = EventManager.EventType.valueOf(typeName);
            EventManager.EventSettings settings = eventManager.getSettings(type);

            int slot = event.getRawSlot();
            switch (slot) {
                case 10:
                    settings.setEnabled(!settings.isEnabled());
                    player.sendMessage("§aСобытие " + type.name() + " теперь " +
                            (settings.isEnabled() ? "§aвключено" : "§cвыключено"));
                    openEventSettings(player, type);
                    break;
                case 11:
                    player.closeInventory();
                    currentInputType = InputType.EVENT_DELAY;
                    editingEvent = type;
                    waitingForInput = player;
                    player.sendMessage("§eВведите задержку до появления (сек):");
                    break;
                case 12:
                    if (type == EventManager.EventType.NUCLEAR) break;
                    player.closeInventory();
                    currentInputType = InputType.EVENT_RADIUS;
                    editingEvent = type;
                    waitingForInput = player;
                    player.sendMessage("§eВведите радиус появления (блоков):");
                    break;
                case 13:
                    player.closeInventory();
                    currentInputType = InputType.COORDINATES;
                    editingEvent = type;
                    waitingForInput = player;
                    player.sendMessage("§eВведите координаты центра через пробел: §6x y z");
                    break;
                case 14:
                    if (type == EventManager.EventType.AIRDROP) {
                        ItemStack itemInHand = player.getInventory().getItemInMainHand();
                        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                            player.sendMessage("§cВозьмите предмет в руку!");
                            return;
                        }
                        tempLootItem = itemInHand.clone();
                        tempLootItem.setAmount(1);
                        player.closeInventory();
                        currentInputType = InputType.LOOT_CHANCE;
                        waitingForInput = player;
                        player.sendMessage("§eВведите шанс выпадения (от 0.01 до 1.0, например 0.5):");
                    } else if (type == EventManager.EventType.BOMBARDMENT) {
                        player.closeInventory();
                        currentInputType = InputType.EVENT_COUNT;
                        editingEvent = type;
                        waitingForInput = player;
                        player.sendMessage("§eВведите количество TNT:");
                    }
                    break;
                case 15:
                    if (type == EventManager.EventType.BOMBARDMENT) {
                        player.closeInventory();
                        currentInputType = InputType.EVENT_INTERVAL;
                        editingEvent = type;
                        waitingForInput = player;
                        player.sendMessage("§eВведите интервал между TNT (тики, 20 тиков = 1 сек):");
                    }
                    break;
                case 16:
                    if (type == EventManager.EventType.NUCLEAR) {
                        player.closeInventory();
                        currentInputType = InputType.EVENT_COUNT;
                        editingEvent = type;
                        waitingForInput = player;
                        player.sendMessage("§eВведите силу взрыва (число от 0.1 до 250.0):");
                    }
                    break;
                case 22:
                    eventManager.startEvent(type);
                    player.sendMessage("§aСобытие " + type.name() + " запущено!");
                    openEventSettings(player, type);
                    break;
                case 24:
                    Location currentLoc = player.getLocation();
                    settings.setCenter(currentLoc);
                    player.sendMessage("§aКоординаты установлены на ваше текущее положение: §6" +
                            currentLoc.getBlockX() + " " + currentLoc.getBlockY() + " " + currentLoc.getBlockZ());
                    eventManager.saveEvents();
                    openEventSettings(player, type);
                    break;
                case 26:
                    openEventsMenu(player);
                    break;
            }
            return;
        }
    }

    // ========================================================================
    // ВЫБОР КОМАНД (ЛКМ – добавить, ПКМ – убрать)
    // ========================================================================
    private void openTeamSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§aВыбор команд");
        inv.setItem(0, createItem(Material.GRASS_BLOCK, "§aВсе игроки",
                "§7Все онлайн игроки",
                "§eЛКМ – выбрать всех | §cПКМ – отменить всех"));

        inv.setItem(53, createItem(Material.LIME_WOOL, "§a§lГОТОВО",
                "§7Сохранить выбранные команды"));

        int slot = 10;
        for (Team team : teamManager.getAllTeams()) {
            if (slot >= 53) break;
            String teamName = team.getName();
            boolean selected = eventManager.getSelectedTeams().contains(teamName);
            Material material = selected ? Material.GREEN_WOOL : Material.WHITE_WOOL;
            String status = selected ? "§a✓ Выбрана" : "§7✗ Не выбрана";
            inv.setItem(slot, createItem(material, "§6" + teamName,
                    "§7Участников: " + team.getMemberCount(),
                    status,
                    "§eЛКМ – добавить | §cПКМ – убрать"));
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
        boolean isLeftClick = event.isLeftClick();
        boolean isRightClick = event.isRightClick();

        if (display.equals("ГОТОВО")) {
            player.closeInventory();
            openMenu(player);
            return;
        }

        if (display.equals("Все игроки")) {
            List<String> teams = eventManager.getSelectedTeams();
            if (isLeftClick) {
                teams.clear();
                teams.add("all");
                player.sendMessage("§aВыбраны все игроки");
            } else if (isRightClick) {
                teams.remove("all");
                player.sendMessage("§cВсе игроки отключены");
            }
            openTeamSelection(player);
            return;
        }

        String teamName = display;
        Team team = teamManager.getTeam(teamName);
        if (team == null) return;

        List<String> selected = eventManager.getSelectedTeams();

        if (isLeftClick) {
            if (!selected.contains(teamName)) {
                selected.remove("all");
                selected.add(teamName);
                player.sendMessage("§aКоманда '" + teamName + "' добавлена");
            } else {
                player.sendMessage("§eКоманда '" + teamName + "' уже выбрана");
            }
        } else if (isRightClick) {
            if (selected.contains(teamName)) {
                selected.remove(teamName);
                player.sendMessage("§cКоманда '" + teamName + "' исключена");
            } else {
                player.sendMessage("§eКоманда '" + teamName + "' не выбрана");
            }
        }

        openTeamSelection(player);
    }

    // ========================================================================
    // ОБРАБОТКА ВВОДА ИЗ ЧАТА (ИСПРАВЛЕНА – добавлен DURATION)
    // ========================================================================
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
            editingEvent = null;
            currentTeamForCoords = null;
            tempLootItem = null;
            openMenu(event.getPlayer());
            return;
        }

        try {
            switch (currentInputType) {
                case DURATION:
                    int minutes = Integer.parseInt(msg);
                    if (minutes <= 0 || minutes > 1440) {
                        waitingForInput.sendMessage("§cВведите число от 1 до 1440 (24 часа)");
                        return;
                    }
                    eventManager.setEventDuration(minutes);
                    waitingForInput.sendMessage("§aДлительность установлена: " + minutes + " минут");
                    break;

                case EVENT_DELAY:
                    int delay = Integer.parseInt(msg);
                    if (delay < 5 || delay > 3600) {
                        waitingForInput.sendMessage("§cОт 5 до 3600 сек");
                        return;
                    }
                    eventManager.getSettings(editingEvent).setDelay(delay);
                    waitingForInput.sendMessage("§aЗадержка установлена: " + delay + " сек");
                    break;

                case EVENT_RADIUS:
                    int radius = Integer.parseInt(msg);
                    if (radius < 5 || radius > 500) {
                        waitingForInput.sendMessage("§cОт 5 до 500 блоков");
                        return;
                    }
                    eventManager.getSettings(editingEvent).setRadius(radius);
                    waitingForInput.sendMessage("§aРадиус установлен: " + radius + " блоков");
                    break;

                case EVENT_COUNT:
                    int count = Integer.parseInt(msg);
                    if (count < 1 || count > 500) {
                        waitingForInput.sendMessage("§cОт 1 до 500");
                        return;
                    }
                    if (editingEvent == null) {
                        eventManager.setCycleInterval(count);
                        waitingForInput.sendMessage("§aИнтервал цикла установлен: " + count + " мин");
                    } else if (editingEvent == EventManager.EventType.BOMBARDMENT) {
                        eventManager.getSettings(editingEvent).setTntCount(count);
                        waitingForInput.sendMessage("§aКоличество TNT: " + count);
                    } else if (editingEvent == EventManager.EventType.NUCLEAR) {
                        float power = Float.parseFloat(msg);
                        if (power < 0.1f || power > 250.0f) {
                            waitingForInput.sendMessage("§cОт 0.1 до 250.0");
                            return;
                        }
                        eventManager.getSettings(editingEvent).setExplosionPower(power);
                        waitingForInput.sendMessage("§aСила взрыва установлена: " + power);
                    }
                    break;

                case EVENT_INTERVAL:
                    int interval = Integer.parseInt(msg);
                    if (interval < 5 || interval > 200) {
                        waitingForInput.sendMessage("§cОт 5 до 200 тиков");
                        return;
                    }
                    eventManager.getSettings(editingEvent).setTntInterval(interval);
                    waitingForInput.sendMessage("§aИнтервал: " + interval + " тиков");
                    break;

                case COORDINATES:
                    String[] parts = msg.split(" ");
                    if (parts.length != 3) {
                        waitingForInput.sendMessage("§cВведите три числа: x y z");
                        return;
                    }
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    double z = Double.parseDouble(parts[2]);
                    Location loc = new Location(waitingForInput.getWorld(), x, y, z);
                    if (editingEvent != null) {
                        eventManager.getSettings(editingEvent).setCenter(loc);
                        waitingForInput.sendMessage("§aКоординаты для события установлены!");
                    } else {
                        eventManager.setCommonCoordinates(loc);
                    }
                    break;

                case LOOT_CHANCE:
                    double chance = Double.parseDouble(msg);
                    if (chance < 0.01 || chance > 1.0) {
                        waitingForInput.sendMessage("§cОт 0.01 до 1.0");
                        return;
                    }
                    if (tempLootItem == null) {
                        waitingForInput.sendMessage("§cОшибка: предмет не выбран!");
                        return;
                    }
                    EventManager.EventSettings airdropSettings = eventManager.getSettings(EventManager.EventType.AIRDROP);
                    airdropSettings.getLootItems().add(new EventManager.EventSettings.LootItem(tempLootItem.clone(), chance));
                    waitingForInput.sendMessage("§aПредмет добавлен в лут с шансом " + chance);
                    tempLootItem = null;
                    eventManager.saveEvents();
                    break;

                default:
                    waitingForInput.sendMessage("§cНеизвестный ввод!");
            }
        } catch (NumberFormatException e) {
            waitingForInput.sendMessage("§cВведите число!");
            return;
        }

        waitingForInput = null;
        currentInputType = InputType.NONE;
        if (editingEvent != null) {
            openEventSettings(event.getPlayer(), editingEvent);
        } else {
            openMenu(event.getPlayer());
        }
    }

    // ========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================================================
    private void startMainEvent(Player player) {
        eventManager.startMainEvent();
        player.closeInventory();
        player.sendMessage("§aИвент запущен!");
    }

    private void openDurationSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Длительность");
        inv.setItem(10, createItem(Material.BOOK, "§eКастомное", "§7Введите в чат минуты"));
        inv.setItem(13, createItem(Material.COOKIE, "§e30 минут"));
        inv.setItem(16, createItem(Material.CAKE, "§e1 час (60 мин)"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onDurationClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6Длительность")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() >= event.getInventory().getSize()) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        if (display.contains("Кастомное")) {
            player.closeInventory();
            currentInputType = InputType.DURATION;
            waitingForInput = player;
            player.sendMessage("§eВведите количество минут (число):");
        } else if (display.contains("30 минут")) {
            eventManager.setEventDuration(30);
            player.sendMessage("§aДлительность: 30 минут");
            player.closeInventory();
            openMenu(player);
        } else if (display.contains("1 час")) {
            eventManager.setEventDuration(60);
            player.sendMessage("§aДлительность: 1 час");
            player.closeInventory();
            openMenu(player);
        }
    }

    private void openWeatherSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§bПогода");
        inv.setItem(10, createItem(Material.SUNFLOWER, "§eЯсно"));
        inv.setItem(13, createItem(Material.WATER_BUCKET, "§bДождь"));
        inv.setItem(16, createItem(Material.LAVA_BUCKET, "§cГроза"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onWeatherClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§bПогода")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() >= event.getInventory().getSize()) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        switch (display) {
            case "Ясно": eventManager.setWeather("clear"); break;
            case "Дождь": eventManager.setWeather("rain"); break;
            case "Гроза": eventManager.setWeather("thunder"); break;
            default: return;
        }
        player.sendMessage("§aПогода: " + display);
        player.closeInventory();
        openMenu(player);
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

    @EventHandler
    public void onTimeClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§dВремя суток")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() >= event.getInventory().getSize()) return;
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
        player.sendMessage("§aВремя суток: " + display);
        player.closeInventory();
        openMenu(player);
    }

    private void openSpawnSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6Координаты команд");
        inv.setItem(0, createItem(Material.BARRIER, "§cСбросить все координаты",
                "§7Удалить координаты для всех команд"));
        inv.setItem(53, createItem(Material.ARROW, "§eНазад",
                "§7Вернуться в главное меню"));

        int slot = 10;
        for (String teamName : eventManager.getSelectedTeams()) {
            if (teamName.equals("all") || slot >= 52) continue;
            Location loc = eventManager.getTeamSpawnLocations().get(teamName);
            String coords = loc == null ? "§cне указаны" : "§6" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
            inv.setItem(slot, createItem(Material.COMPASS, "§6" + teamName,
                    "§7Координаты: " + coords,
                    "§eНажмите, чтобы установить",
                    "§7(введите в чат: x y z)"));
            slot++;
        }
        if (slot == 10) {
            inv.setItem(22, createItem(Material.RED_STAINED_GLASS_PANE, "§cНет выбранных команд",
                    "§7Сначала выберите команды в главном меню"));
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onSpawnSettingsClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6Координаты команд")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() >= event.getInventory().getSize()) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
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
        String teamName = display;
        if (teamManager.getTeam(teamName) != null) {
            player.closeInventory();
            currentInputType = InputType.COORDINATES;
            currentTeamForCoords = teamName;
            waitingForInput = player;
            player.sendMessage("§eВведите координаты для команды §6" + teamName + " §eчерез пробел: §6x y z");
            player.sendMessage("§7(например: 100 64 200) или напишите §cотмена");
        }
    }

    private void promptEndSpawnCoordinates(Player player) {
        player.closeInventory();
        currentInputType = InputType.COORDINATES;
        waitingForInput = player;
        currentTeamForCoords = null;
        player.sendMessage("§eВведите координаты спавна после ивента: §6x y z");
        player.sendMessage("§7(или напишите §cотмена§7)");
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String getTeamsDisplay() {
        List<String> teams = eventManager.getSelectedTeams();
        return teams.isEmpty() ? "§cне выбраны" : String.join(", ", teams);
    }

    private String getDurationDisplay() {
        int min = eventManager.getEventDuration();
        return min <= 0 ? "§cНе установлено" : "§6" + min + " мин";
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
        return eventManager.getTeamSpawnLocations().size() + " команд";
    }

    private String getEndSpawnDisplay() {
        Location loc = eventManager.getEndSpawnLocation();
        return loc == null ? "§cне указаны" : "§6" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }
}