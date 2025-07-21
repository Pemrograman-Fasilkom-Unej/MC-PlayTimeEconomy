package com.darkun7.timerald.shop;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ShopItem implements ConfigurationSerializable {

    private ItemStack item;
    private int price;
    private int quantity;

    public ShopItem(ItemStack item, int price, int quantity) {
        this.item = item.clone();
        this.price = price;
        this.quantity = quantity;
        this.item.setAmount(quantity); // Ensure correct quantity
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

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("item", item);
        map.put("price", price);
        map.put("quantity", quantity);
        return map;
    }

    public static ShopItem deserialize(Map<?, ?> map) {
        ItemStack item = (ItemStack) map.get("item");
        int price = (int) map.get("price");
        int quantity = (int) map.get("quantity");
        return new ShopItem(item, price, quantity);
    }
}
