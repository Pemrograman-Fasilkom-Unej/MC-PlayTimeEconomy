package com.darkun7.timerald.shop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerShop {

    private final UUID owner;
    private final List<ShopItem> listings = new ArrayList<>();
    private final List<ItemStack> stash = new ArrayList<>();

    public PlayerShop(UUID owner) {
        this.owner = owner;
    }

    public UUID getOwner() {
        return owner;
    }

    public List<ShopItem> getListings() {
        return listings;
    }

    public List<ItemStack> getStash() {
        return stash;
    }

    public void addListing(ShopItem item) {
        listings.add(item);
    }

    public boolean cancelListing(int index) {
        if (index >= 0 && index < listings.size()) {
            listings.remove(index);
            return true;
        }
        return false;
    }

    public ShopItem getListing(int index) {
        if (index >= 0 && index < listings.size()) {
            return listings.get(index);
        }
        return null;
    }

    public boolean removeItemFromStash(ItemStack toRemove, int quantity) {
        int remaining = quantity;

        Iterator<ItemStack> iterator = stash.iterator();

        while (iterator.hasNext()) {
            ItemStack current = iterator.next();

            if (current != null && current.isSimilar(toRemove)) {
                int amount = current.getAmount();

                if (amount > remaining) {
                    current.setAmount(amount - remaining);
                    return true;
                } else {
                    remaining -= amount;
                    iterator.remove(); // remove entire stack
                    if (remaining <= 0) return true;
                }
            }
        }

        // Not enough items
        return false;
    }

    public void addToStash(ItemStack item) {
        stash.add(item);
    }
}
