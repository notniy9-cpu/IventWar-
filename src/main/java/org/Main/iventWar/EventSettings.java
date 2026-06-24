package org.Main.iventWar;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EventSettings {
    private EventType type;
    private boolean enabled;
    private int delay; // секунды до первого появления
    private int interval; // интервал между появлениями (сек)
    private int duration; // длительность (сек)
    private Location center; // центр зоны
    private int radius; // радиус зоны
    private List<LootItem> lootItems; // предметы с шансами
    private List<TradeItem> tradeItems; // предметы для торговца
    private int tntCount; // количество TNT (для бомбардировки)
    private int tntInterval; // интервал между TNT (тики)

    public EventSettings(EventType type) {
        this.type = type;
        this.enabled = false;
        this.delay = 60;
        this.interval = 120;
        this.duration = 60;
        this.radius = 50;
        this.lootItems = new ArrayList<>();
        this.tradeItems = new ArrayList<>();
        this.tntCount = 20;
        this.tntInterval = 10;
    }

    // Геттеры и сеттеры
    public EventType getType() { return type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public Location getCenter() { return center; }
    public void setCenter(Location center) { this.center = center; }
    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; }
    public List<LootItem> getLootItems() { return lootItems; }
    public void setLootItems(List<LootItem> lootItems) { this.lootItems = lootItems; }
    public List<TradeItem> getTradeItems() { return tradeItems; }
    public void setTradeItems(List<TradeItem> tradeItems) { this.tradeItems = tradeItems; }
    public int getTntCount() { return tntCount; }
    public void setTntCount(int tntCount) { this.tntCount = tntCount; }
    public int getTntInterval() { return tntInterval; }
    public void setTntInterval(int tntInterval) { this.tntInterval = tntInterval; }

    // Класс для предметов с шансом
    public static class LootItem {
        private ItemStack item;
        private double chance; // 0.0 - 1.0

        public LootItem(ItemStack item, double chance) {
            this.item = item;
            this.chance = chance;
        }

        public ItemStack getItem() { return item; }
        public double getChance() { return chance; }
        public void setItem(ItemStack item) { this.item = item; }
        public void setChance(double chance) { this.chance = chance; }
    }

    // Класс для предметов торговца
    public static class TradeItem {
        private ItemStack buyItem;
        private ItemStack sellItem;
        private int buyAmount;
        private int sellAmount;

        public TradeItem(ItemStack buyItem, int buyAmount, ItemStack sellItem, int sellAmount) {
            this.buyItem = buyItem;
            this.buyAmount = buyAmount;
            this.sellItem = sellItem;
            this.sellAmount = sellAmount;
        }

        public ItemStack getBuyItem() { return buyItem; }
        public int getBuyAmount() { return buyAmount; }
        public ItemStack getSellItem() { return sellItem; }
        public int getSellAmount() { return sellAmount; }
    }
}

enum EventType {
    AIRDROP,
    MYSTERY_TRADER,
    BOMBARDMENT
}
