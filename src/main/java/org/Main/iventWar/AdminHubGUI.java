package org.Main.iventWar;

import org.bukkit.BanList;
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
import java.util.UUID;

public class AdminHubGUI implements Listener {
    private final IventWar plugin;
    private Player waitingForInput;
    private enum InputType { NONE, BAN_NAME, IP_BAN_NAME, ANNOUNCEMENT }
    private InputType currentInputType = InputType.NONE;

    public AdminHubGUI(IventWar plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lАДМИН-ПАНЕЛЬ");

        // Палочка бана
        inv.setItem(10, createItem(Material.BLAZE_ROD, "§cПалочка бана",
                "§7Нажмите, чтобы забанить игрока",
                "§7(введите имя в чат)"));

        // Палочка IP-бана
        inv.setItem(13, createItem(Material.STICK, "§6IP-бан",
                "§7Нажмите, чтобы забанить по IP",
                "§7(введите имя в чат)"));

        // Кнопка объявления
        inv.setItem(16, createItem(Material.PAPER, "§cОбъявление",
                "§7Нажмите, чтобы сделать объявление",
                "§7(введите текст в чат)"));

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
                currentInputType = InputType.BAN_NAME;
                waitingForInput = player;
                player.sendMessage("§eВведите имя игрока для бана (или §cотмена§e):");
                break;
            case 13:
                player.closeInventory();
                currentInputType = InputType.IP_BAN_NAME;
                waitingForInput = player;
                player.sendMessage("§eВведите имя игрока для IP-бана (или §cотмена§e):");
                break;
            case 16:
                player.closeInventory();
                currentInputType = InputType.ANNOUNCEMENT;
                waitingForInput = player;
                player.sendMessage("§eВведите текст объявления (или §cотмена§e):");
                break;
        }
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
            openMenu(event.getPlayer());
            return;
        }

        switch (currentInputType) {
            case BAN_NAME:
                banPlayer(event.getPlayer(), msg);
                break;
            case IP_BAN_NAME:
                ipBanPlayer(event.getPlayer(), msg);
                break;
            case ANNOUNCEMENT:
                sendAnnouncement(event.getPlayer(), msg);
                break;
            default:
                break;
        }

        waitingForInput = null;
        currentInputType = InputType.NONE;
        openMenu(event.getPlayer());
    }

    private void banPlayer(Player admin, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            admin.sendMessage("§cИгрок не найден!");
            return;
        }
        // Бан навсегда
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), "§cЗабанен администратором", null, null);
        target.kickPlayer("§cВы были забанены администратором!");
        admin.sendMessage("§aИгрок " + target.getName() + " забанен навсегда!");
        Bukkit.broadcastMessage("§c" + target.getName() + " был забанен администратором!");
    }

    private void ipBanPlayer(Player admin, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            admin.sendMessage("§cИгрок не найден!");
            return;
        }
        String ip = target.getAddress().getAddress().getHostAddress();
        Bukkit.getBanList(BanList.Type.IP).addBan(ip, "§cIP-бан администратором", null, null);
        target.kickPlayer("§cВаш IP-адрес был забанен!");
        admin.sendMessage("§aIP-адрес " + ip + " забанен навсегда!");
        Bukkit.broadcastMessage("§c" + target.getName() + " был забанен по IP!");
    }

    private void sendAnnouncement(Player admin, String text) {
        String message = "§c§l[ОБЪЯВЛЕНИЕ] §f" + text;
        Bukkit.broadcastMessage(message);
        admin.sendMessage("§aОбъявление отправлено!");
    }
}
