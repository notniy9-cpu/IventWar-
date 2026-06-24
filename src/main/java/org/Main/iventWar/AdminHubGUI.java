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
            // Проверяем, является ли ввод именем игрока или названием цвета
            Player target = Bukkit.getPlayer(msg);
            if (target != null) {
                targetPlayerName = msg;
                waitingForInput.sendMessage("§eТеперь введите название цвета в чат (red, green, blue, yellow, gold, aqua, dark_red, dark_green, dark_blue, dark_purple, black, white, gray):");
                currentInputType = InputType.ANNOUNCEMENT; // временно используем как ожидание цвета
                waitingForInput = event.getPlayer();
                return;
            } else {
                // Если это не игрок, то возможно это цвет
                ChatColor color = getColorByName(msg);
                if (color != null) {
                    if (targetPlayerName == null) {
                        waitingForInput.sendMessage("§cСначала введите имя игрока!");
                        return;
                    }
                    Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                    if (targetPlayer != null) {
                        playerChatColors.put(targetPlayer.getUniqueId(), color);
                        waitingForInput.sendMessage("§aЦвет для " + targetPlayerName + " изменён на " + color + msg);
                    } else {
                        waitingForInput.sendMessage("§cИгрок не найден!");
                    }
                    waitingForInput = null;
                    currentInputType = InputType.NONE;
                    targetPlayerName = null;
                    openMenu(event.getPlayer());
                } else {
                    waitingForInput.sendMessage("§cИгрок не найден и это не название цвета!");
                    waitingForInput.sendMessage("§7Доступные цвета: red, green, blue, yellow, gold, aqua, dark_red, dark_green, dark_blue, dark_purple, black, white, gray");
                }
            }
        } else if (currentInputType == InputType.ANNOUNCEMENT) {
            sendAnnouncement(event.getPlayer(), msg);
            waitingForInput = null;
            currentInputType = InputType.NONE;
            openMenu(event.getPlayer());
        }
    }

    private ChatColor getColorByName(String name) {
        name = name.toLowerCase();
        switch (name) {
            case "red": return ChatColor.RED;
            case "green": return ChatColor.GREEN;
            case "blue": return ChatColor.BLUE;
            case "yellow": return ChatColor.YELLOW;
            case "gold": return ChatColor.GOLD;
            case "aqua": return ChatColor.AQUA;
            case "dark_red": return ChatColor.DARK_RED;
            case "dark_green": return ChatColor.DARK_GREEN;
            case "dark_blue": return ChatColor.DARK_BLUE;
            case "dark_purple": return ChatColor.DARK_PURPLE;
            case "black": return ChatColor.BLACK;
            case "white": return ChatColor.WHITE;
            case "gray": return ChatColor.GRAY;
            default: return null;
        }
    }

    private void sendAnnouncement(Player admin, String text) {
        String message = "§c§l[ОБЪЯВЛЕНИЕ] §f" + text;
        Bukkit.broadcastMessage(message);
        admin.sendMessage("§aОбъявление отправлено!");
    }

    public ChatColor getPlayerChatColor(Player player) {
        return playerChatColors.getOrDefault(player.getUniqueId(), ChatColor.WHITE);
    }
}