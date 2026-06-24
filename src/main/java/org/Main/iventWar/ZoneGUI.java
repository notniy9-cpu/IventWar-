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

import java.util.Arrays;

public class ZoneGUI implements Listener {
    private final IventWar plugin;
    private final ZoneManager zoneManager;
    private Player waitingForInput;
    private enum InputType { NONE, ZONE_NAME, ZONE_RADIUS, ZONE_HEIGHT, ZONE_TIME, ZONE_COORDS }
    private InputType currentInputType = InputType.NONE;
    private String tempName;
    private int tempRadius;
    private int tempHeight = 5;
    private int tempTime;
    private Location tempCoords;
    private Zone editingZone;

    public ZoneGUI(IventWar plugin) {
        this.plugin = plugin;
        this.zoneManager = plugin.getZoneManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ----- Меню создания зоны -----
    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, "§6§lСОЗДАНИЕ ЗОНЫ");

        // Название
        inv.setItem(10, createItem(Material.NAME_TAG, "§aНазвание зоны",
                "§7Текущее: " + (tempName == null ? "§cне указано" : "§6" + tempName),
                "§eНажмите, чтобы ввести название"));

        // Координаты
        String coordsDisplay = (tempCoords == null) ? "§cиспользовать текущие" :
                "§6" + tempCoords.getBlockX() + " " + tempCoords.getBlockY() + " " + tempCoords.getBlockZ();
        inv.setItem(11, createItem(Material.COMPASS, "§6Координаты зоны",
                "§7Текущие: " + coordsDisplay,
                "§eНажмите, чтобы указать координаты",
                "§7(введите в чат: x y z)"));

        // Радиус
        inv.setItem(13, createItem(Material.COMPASS, "§6Ширина зоны (радиус)",
                "§7Текущий: " + (tempRadius == 0 ? "§cне указан" : "§6" + tempRadius + " блоков"),
                "§eНажмите, чтобы ввести радиус"));

        // Высота
        inv.setItem(14, createItem(Material.PISTON, "§6Высота зоны",
                "§7Текущая: " + tempHeight + " блоков",
                "§eНажмите, чтобы изменить высоту"));

        // Время захвата
        inv.setItem(16, createItem(Material.CLOCK, "§dДлительность захвата",
                "§7Текущая: " + (tempTime == 0 ? "§cне указана" : "§6" + tempTime + " сек"),
                "§eНажмите, чтобы ввести время"));

        // Кнопка создания
        inv.setItem(22, createItem(Material.EMERALD_BLOCK, "§a§lСОЗДАТЬ ЗОНУ",
                "§7Все настройки будут применены",
                "§eНажмите для создания!"));

        player.openInventory(inv);
    }

    // ----- Меню просмотра всех зон -----
    public void openZoneList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lСПИСОК ЗОН");

        int slot = 10;
        for (Zone zone : zoneManager.getZones()) {
            if (slot >= 52) break;
            String owner = zone.getOwnerTeam();
            String ownerDisplay = (owner == null) ? "§7Нейтральная" : "§6" + owner;
            ItemStack item = createItem(Material.WHITE_BANNER, "§6" + zone.getName(),
                    "§7Владелец: " + ownerDisplay,
                    "§7Радиус: " + zone.getRadius() + " | Высота: " + zone.getHeight(),
                    "§7Время захвата: " + zone.getCaptureTime() + " сек",
                    "§7Координаты: " + zone.getCenter().getBlockX() + " " + zone.getCenter().getBlockY() + " " + zone.getCenter().getBlockZ(),
                    "§eЛКМ – редактировать | §cПКМ – удалить");
            inv.setItem(slot, item);
            slot++;
        }

        if (slot == 10) {
            inv.setItem(22, createItem(Material.RED_STAINED_GLASS_PANE, "§cНет созданных зон",
                    "§7Используйте /createzone для создания"));
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    // ----- Обработка кликов в меню создания -----
    @EventHandler
    public void onCreateMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lСОЗДАНИЕ ЗОНЫ")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        switch (event.getRawSlot()) {
            case 10: promptName(player); break;
            case 11: promptCoords(player); break;
            case 13: promptRadius(player); break;
            case 14: promptHeight(player); break;
            case 16: promptTime(player); break;
            case 22: createZone(player); break;
        }
    }

    // ----- Обработка кликов в списке зон -----
    @EventHandler
    public void onZoneListClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lСПИСОК ЗОН")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        Zone zone = zoneManager.getZone(display);
        if (zone == null) return;

        if (event.isLeftClick()) {
            // Редактирование
            openEditMenu(player, zone);
        } else if (event.isRightClick()) {
            // Удаление
            zoneManager.deleteZone(zone.getName());
            player.sendMessage("§cЗона '" + zone.getName() + "' удалена!");
            openZoneList(player);
        }
    }

    // ----- Меню редактирования зоны -----
    public void openEditMenu(Player player, Zone zone) {
        editingZone = zone;
        tempName = zone.getName();
        tempRadius = zone.getRadius();
        tempHeight = zone.getHeight();
        tempTime = zone.getCaptureTime();
        tempCoords = zone.getCenter();

        Inventory inv = Bukkit.createInventory(null, 36, "§6§lРЕДАКТИРОВАНИЕ: " + zone.getName());

        inv.setItem(10, createItem(Material.NAME_TAG, "§aНазвание",
                "§7Текущее: §6" + tempName,
                "§eНажмите, чтобы изменить"));

        String coordsDisplay = "§6" + tempCoords.getBlockX() + " " + tempCoords.getBlockY() + " " + tempCoords.getBlockZ();
        inv.setItem(11, createItem(Material.COMPASS, "§6Координаты",
                "§7Текущие: " + coordsDisplay,
                "§eНажмите, чтобы изменить"));

        inv.setItem(13, createItem(Material.COMPASS, "§6Радиус",
                "§7Текущий: " + tempRadius + " блоков",
                "§eНажмите, чтобы изменить"));

        inv.setItem(14, createItem(Material.PISTON, "§6Высота",
                "§7Текущая: " + tempHeight + " блоков",
                "§eНажмите, чтобы изменить"));

        inv.setItem(16, createItem(Material.CLOCK, "§dВремя захвата",
                "§7Текущее: " + tempTime + " сек",
                "§eНажмите, чтобы изменить"));

        inv.setItem(22, createItem(Material.LIME_WOOL, "§a§lСОХРАНИТЬ ИЗМЕНЕНИЯ",
                "§7Применить изменения к зоне",
                "§eНажмите для сохранения!"));

        inv.setItem(31, createItem(Material.BARRIER, "§c§lОТМЕНА",
                "§7Вернуться к списку зон"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onEditMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("§6§lРЕДАКТИРОВАНИЕ:")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        switch (event.getRawSlot()) {
            case 10: promptName(player); break;
            case 11: promptCoords(player); break;
            case 13: promptRadius(player); break;
            case 14: promptHeight(player); break;
            case 16: promptTime(player); break;
            case 22:
                if (editingZone == null) { player.sendMessage("§cОшибка!"); return; }
                editingZone.setCenter(tempCoords);
                editingZone.setRadius(tempRadius);
                editingZone.setHeight(tempHeight);
                editingZone.setCaptureTime(tempTime);
                if (!editingZone.getName().equals(tempName)) {
                    zoneManager.deleteZone(editingZone.getName());
                    zoneManager.createZone(tempName, tempCoords, tempRadius, tempHeight, tempTime);
                    player.sendMessage("§aЗона переименована в '" + tempName + "'!");
                } else {
                    zoneManager.saveZones();
                    player.sendMessage("§aИзменения сохранены!");
                }
                editingZone = null;
                player.closeInventory();
                openZoneList(player);
                break;
            case 31:
                editingZone = null;
                player.closeInventory();
                openZoneList(player);
                break;
        }
    }

    // ----- Вспомогательные методы для ввода -----
    private void promptName(Player player) {
        player.closeInventory();
        currentInputType = InputType.ZONE_NAME;
        waitingForInput = player;
        player.sendMessage("§eВведите название зоны в чат (или §cотмена§e):");
    }

    private void promptCoords(Player player) {
        player.closeInventory();
        currentInputType = InputType.ZONE_COORDS;
        waitingForInput = player;
        player.sendMessage("§eВведите координаты через пробел: §6x y z §e(или §cотмена§e):");
    }

    private void promptRadius(Player player) {
        player.closeInventory();
        currentInputType = InputType.ZONE_RADIUS;
        waitingForInput = player;
        player.sendMessage("§eВведите радиус (число блоков, 1-100) (или §cотмена§e):");
    }

    private void promptHeight(Player player) {
        player.closeInventory();
        currentInputType = InputType.ZONE_HEIGHT;
        waitingForInput = player;
        player.sendMessage("§eВведите высоту (число блоков, 1-50) (или §cотмена§e):");
    }

    private void promptTime(Player player) {
        player.closeInventory();
        currentInputType = InputType.ZONE_TIME;
        waitingForInput = player;
        player.sendMessage("§eВведите длительность захвата (сек, 1-600) (или §cотмена§e):");
    }

    private void createZone(Player player) {
        if (tempName == null || tempName.isEmpty()) { player.sendMessage("§cУкажите название!"); return; }
        if (tempRadius <= 0) { player.sendMessage("§cУкажите радиус!"); return; }
        if (tempTime <= 0) { player.sendMessage("§cУкажите время захвата!"); return; }
        Location center = (tempCoords != null) ? tempCoords : player.getLocation();
        zoneManager.createZone(tempName, center, tempRadius, tempHeight, tempTime);
        player.sendMessage("§aЗона '" + tempName + "' создана!");
        player.closeInventory();
        tempName = null; tempRadius = 0; tempHeight = 5; tempTime = 0; tempCoords = null;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (waitingForInput == null) return;
        if (!event.getPlayer().equals(waitingForInput)) return;
        event.setCancelled(true);

        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("отмена")) {
            waitingForInput.sendMessage("§cВвод отменён");
            waitingForInput = null; currentInputType = InputType.NONE;
            if (editingZone != null) openEditMenu(event.getPlayer(), editingZone);
            else openMenu(event.getPlayer());
            return;
        }

        switch (currentInputType) {
            case ZONE_NAME:
                if (zoneManager.getZone(msg) != null && (editingZone == null || !editingZone.getName().equals(msg))) {
                    waitingForInput.sendMessage("§cЗона с таким названием уже существует!");
                    return;
                }
                tempName = msg;
                waitingForInput.sendMessage("§aНазвание: §6" + msg);
                waitingForInput = null; currentInputType = InputType.NONE;
                if (editingZone != null) openEditMenu(event.getPlayer(), editingZone);
                else openMenu(event.getPlayer());
                break;

            case ZONE_COORDS:
                String[] parts = msg.split(" ");
                if (parts.length != 3) { waitingForInput.sendMessage("§cВведите три числа: x y z"); return; }
                try {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    double z = Double.parseDouble(parts[2]);
                    tempCoords = new Location(waitingForInput.getWorld(), x, y, z);
                    waitingForInput.sendMessage("§aКоординаты: " + tempCoords.getBlockX() + " " + tempCoords.getBlockY() + " " + tempCoords.getBlockZ());
                    waitingForInput = null; currentInputType = InputType.NONE;
                    if (editingZone != null) openEditMenu(event.getPlayer(), editingZone);
                    else openMenu(event.getPlayer());
                } catch (NumberFormatException e) { waitingForInput.sendMessage("§cВведите числа!"); }
                break;

            case ZONE_RADIUS:
                try {
                    int radius = Integer.parseInt(msg);
                    if (radius <= 0 || radius > 100) { waitingForInput.sendMessage("§cОт 1 до 100"); return; }
                    tempRadius = radius;
                    waitingForInput.sendMessage("§aРадиус: " + radius + " блоков");
                    waitingForInput = null; currentInputType = InputType.NONE;
                    if (editingZone != null) openEditMenu(event.getPlayer(), editingZone);
                    else openMenu(event.getPlayer());
                } catch (NumberFormatException e) { waitingForInput.sendMessage("§cВведите число!"); }
                break;

            case ZONE_HEIGHT:
                try {
                    int height = Integer.parseInt(msg);
                    if (height <= 0 || height > 50) { waitingForInput.sendMessage("§cОт 1 до 50"); return; }
                    tempHeight = height;
                    waitingForInput.sendMessage("§aВысота: " + height + " блоков");
                    waitingForInput = null; currentInputType = InputType.NONE;
                    if (editingZone != null) openEditMenu(event.getPlayer(), editingZone);
                    else openMenu(event.getPlayer());
                } catch (NumberFormatException e) { waitingForInput.sendMessage("§cВведите число!"); }
                break;

            case ZONE_TIME:
                try {
                    int time = Integer.parseInt(msg);
                    if (time <= 0 || time > 600) { waitingForInput.sendMessage("§cОт 1 до 600"); return; }
                    tempTime = time;
                    waitingForInput.sendMessage("§aВремя захвата: " + time + " сек");
                    waitingForInput = null; currentInputType = InputType.NONE;
                    if (editingZone != null) openEditMenu(event.getPlayer(), editingZone);
                    else openMenu(event.getPlayer());
                } catch (NumberFormatException e) { waitingForInput.sendMessage("§cВведите число!"); }
                break;

            default: break;
        }
    }
}