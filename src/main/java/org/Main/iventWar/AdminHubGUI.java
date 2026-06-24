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

import java.util.*;

public class AdminHubGUI implements Listener {
    private final IventWar plugin;
    private Player waitingForInput;
    private enum InputType { NONE, COLOR_PLAYER, ANNOUNCEMENT }
    private InputType currentInputType = InputType.NONE;
    private String targetPlayerName;

    private final Map<UUID, ChatColor> playerChatColors = new HashMap<>();

    // Цвета и соответствующие материалы для наглядности
    private final List<ColorOption> colorOptions = Arrays.asList(
            new ColorOption("§cКрасный", ChatColor.RED, Material.RED_WOOL),
            new ColorOption("§6Золотой", ChatColor.GOLD, Material.ORANGE_WOOL),
            new ColorOption("§eЖёлтый", ChatColor.YELLOW, Material.YELLOW_WOOL),
            new ColorOption("§aЗелёный", ChatColor.GREEN, Material.GREEN_WOOL),
            new ColorOption("§bГолубой", ChatColor.AQUA, Material.LIGHT_BLUE_WOOL),
            new ColorOption("§9Синий", ChatColor.BLUE, Material.BLUE_WOOL),
            new ColorOption("§1Тёмно-синий", ChatColor.DARK_BLUE, Material.CYAN_WOOL),
            new ColorOption("§3Тёмно-бирюзовый", ChatColor.DARK_AQUA, Material.CYAN_WOOL),
            new ColorOption("§2Тёмно-зелёный", ChatColor.DARK_GREEN, Material.GREEN_WOOL),
            new ColorOption("§5Фиолетовый", ChatColor.DARK_PURPLE, Material.PURPLE_WOOL),
            new ColorOption("§dРозовый", ChatColor.LIGHT_PURPLE, Material.MAGENTA_WOOL),
            new ColorOption("§7Серый", ChatColor.GRAY, Material.GRAY_WOOL),
            new ColorOption("§8Тёмно-серый", ChatColor.DARK_GRAY, Material.GRAY_WOOL),
            new ColorOption("§0Чёрный", ChatColor.BLACK, Material.BLACK_WOOL),
            new ColorOption("§fБелый", ChatColor.WHITE, Material.WHITE_WOOL)
    );

    public AdminHubGUI(IventWar plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lАДМИН-ПАНЕЛЬ");

        inv.setItem(10, createItem(Material.INK_SAC, "§aИзменить цвет сообщений",
                "§7Выбрать игрока и задать ему цвет чата",
                "§eНажмите, чтобы начать"));

        inv.setItem(13, createItem(Material.CHEST, "§6Админские предметы",
                "§7Открыть меню с полезными предметами",
                "§eНажмите, чтобы взять предметы"));

        inv.setItem(16, createItem(Material.PAPER, "§cОбъявление",
                "§7Отправить объявление всем игрокам",
                "§eНажмите, чтобы ввести текст"));

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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lАДМИН-ПАНЕЛЬ")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        switch (slot) {
            case 10:
                player.closeInventory();
                currentInputType = InputType.COLOR_PLAYER;
                waitingForInput = player;
                player.sendMessage("§eВведите имя игрока, которому хотите изменить цвет чата:");
                player.sendMessage("§7(или напишите §cотмена§7)");
                break;
            case 13:
                openAdminItems(player);
                break;
            case 16:
                player.closeInventory();
                currentInputType = InputType.ANNOUNCEMENT;
                waitingForInput = player;
                player.sendMessage("§eВведите текст объявления:");
                player.sendMessage("§7(или напишите §cотмена§7)");
                break;
        }
    }

    private void openAdminItems(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Админские предметы");
        inv.setItem(10, createItem(Material.BEACON, "§6Маяк", "§7Выдаёт маяк"));
        inv.setItem(11, createItem(Material.END_CRYSTAL, "§dКристалл Энда", "§7Выдаёт кристалл"));
        inv.setItem(12, createItem(Material.NETHER_STAR, "§eЗвезда Незера", "§7Выдаёт звезду"));
        inv.setItem(13, createItem(Material.DRAGON_HEAD, "§5Голова Дракона", "§7Выдаёт голову дракона"));
        inv.setItem(14, createItem(Material.STRUCTURE_BLOCK, "§aСтруктурный блок", "§7Выдаёт структурный блок"));
        inv.setItem(15, createItem(Material.RESPAWN_ANCHOR, "§cЯкорь возрождения", "§7Выдаёт якорь"));
        inv.setItem(16, createItem(Material.CONDUIT, "§bМорской проводник", "§7Выдаёт проводник"));
        inv.setItem(22, createItem(Material.BARRIER, "§cНазад", "§7Вернуться в админ-панель"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onAdminItemsClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6Админские предметы")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        if (slot == 22) {
            player.closeInventory();
            openMenu(player);
            return;
        }
        ItemStack item = event.getCurrentItem().clone();
        item.setAmount(1);
        player.getInventory().addItem(item);
        player.sendMessage("§aВы получили " + item.getItemMeta().getDisplayName());
        player.closeInventory();
        openMenu(player);
    }

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
            targetPlayerName = null;
            openMenu(event.getPlayer());
            return;
        }

        if (currentInputType == InputType.COLOR_PLAYER) {
            targetPlayerName = msg;
            waitingForInput.sendMessage("§eТеперь выберите цвет из меню:");
            openColorSelection(event.getPlayer());
            waitingForInput = null;
            currentInputType = InputType.NONE;
        } else if (currentInputType == InputType.ANNOUNCEMENT) {
            sendAnnouncement(event.getPlayer(), msg);
            waitingForInput = null;
            currentInputType = InputType.NONE;
            openMenu(event.getPlayer());
        }
    }

    // ----- Упрощённое меню выбора цвета с подписями -----
    private void openColorSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§aВыбор цвета для " + targetPlayerName);

        int slot = 10;
        for (ColorOption option : colorOptions) {
            if (slot > 25) break;
            ItemStack item = new ItemStack(option.material, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(option.displayName);
            meta.setLore(Arrays.asList("§7Нажмите, чтобы выбрать этот цвет"));
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }

        // Кнопка сброса
        inv.setItem(22, createItem(Material.BARRIER, "§cСбросить цвет",
                "§7Вернуть обычный цвет (белый)"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onColorClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§aВыбор цвета для " + targetPlayerName)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        if (slot == 22) {
            // Сброс
            Player target = Bukkit.getPlayer(targetPlayerName);
            if (target != null) {
                playerChatColors.remove(target.getUniqueId());
                player.sendMessage("§aЦвет для " + targetPlayerName + " сброшен.");
            } else {
                player.sendMessage("§cИгрок не найден!");
            }
            player.closeInventory();
            openMenu(player);
            targetPlayerName = null;
            return;
        }

        // Определяем цвет по названию предмета (с учётом цветового кода)
        String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
        ChatColor selectedColor = null;
        for (ColorOption option : colorOptions) {
            if (option.displayName.equals(displayName)) {
                selectedColor = option.color;
                break;
            }
        }

        if (selectedColor == null) {
            player.sendMessage("§cОшибка выбора цвета!");
            return;
        }

        Player target = Bukkit.getPlayer(targetPlayerName);
        if (target != null) {
            playerChatColors.put(target.getUniqueId(), selectedColor);
            player.sendMessage("§aЦвет для " + targetPlayerName + " изменён на " + selectedColor + displayName);
        } else {
            player.sendMessage("§cИгрок не найден!");
        }
        player.closeInventory();
        openMenu(player);
        targetPlayerName = null;
    }

    private void sendAnnouncement(Player admin, String text) {
        String message = "§c§l[ОБЪЯВЛЕНИЕ] §f" + text;
        Bukkit.broadcastMessage(message);
        admin.sendMessage("§aОбъявление отправлено!");
    }

    public ChatColor getPlayerChatColor(Player player) {
        return playerChatColors.getOrDefault(player.getUniqueId(), ChatColor.WHITE);
    }

    // Вспомогательный класс для хранения цветов
    private static class ColorOption {
        String displayName;
        ChatColor color;
        Material material;
        ColorOption(String displayName, ChatColor color, Material material) {
            this.displayName = displayName;
            this.color = color;
            this.material = material;
        }
    }
}