package com.darkun7.timerald.shop;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class OrderItem implements ConfigurationSerializable {

    private ItemStack item;
    private int price;
    private int quantity;
    private int limit;

    public OrderItem(ItemStack item, int price, int quantity, int limit) {
        this.item = item.clone();
        this.price = price;
        this.quantity = quantity;
        this.item.setAmount(quantity); // Ensure correct quantity
        this.limit = limit;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void reduceQuantity(int amount) {
        this.quantity -= amount;
        if (this.quantity < 0) this.quantity = 0;
    }

    public int getLimit() {
        return limit;
    }

    public int setLimit(int amount) {
        return this.limit = amount;
    }

    public void reduceLimit(int amount) {
        this.limit -= amount;
        if (this.limit < 0) this.limit = 0;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("item", item);
        map.put("price", price);
        map.put("quantity", quantity);
        map.put("limit", limit);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static OrderItem deserialize(Map<?, ?> map) {
        if (map == null) return null;

        ItemStack item = (ItemStack) (map.get("item") != null ? map.get("item") : new ItemStack(Material.AIR));
        int price = map.get("price") != null ? ((Number) map.get("price")).intValue() : 0;
        int quantity = map.get("quantity") != null ? ((Number) map.get("quantity")).intValue() : 1;
        int limit = map.get("limit") != null ? ((Number) map.get("limit")).intValue() : 0;

        return new OrderItem(item, price, quantity, limit);
    }

}
