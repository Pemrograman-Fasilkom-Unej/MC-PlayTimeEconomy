package com.darkun7.timerald.shop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerShop {

    private static final int MAX_STASH_SIZE = 54;

    private final UUID owner;
    private final List<ShopItem> listings = new ArrayList<>();
    private final List<OrderItem> requests = new ArrayList<>();
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

    public List<OrderItem> getRequests() {
        return requests;
    }

    public List<ItemStack> getStash() {
        return stash;
    }

    public void addListing(ShopItem item) {
        listings.add(item);
    }

    public void addRequest(OrderItem request) {
        requests.add(request);
    }

    public boolean cancelListing(int index) {
        if (index >= 0 && index < listings.size()) {
            listings.remove(index);
            return true;
        }
        return false;
    }

    public boolean cancelRequest(int index) {
        if (index >= 0 && index < requests.size()) {
            requests.remove(index);
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

    public OrderItem getRequest(int index) {
        if (index >= 0 && index < requests.size()) {
            return requests.get(index);
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

    public boolean canAddToStash(ItemStack itemToAdd) {
        int amountToAdd = itemToAdd.getAmount();

        // Try merging with existing stacks first
        for (ItemStack stack : stash) {
            if (stack != null && stack.isSimilar(itemToAdd)) {
                int maxStackSize = stack.getMaxStackSize();
                int freeSpace = maxStackSize - stack.getAmount();
                amountToAdd -= freeSpace;
                if (amountToAdd <= 0) return true;
            }
        }

        // Calculate free slots after merging
        int freeSlots = MAX_STASH_SIZE - stash.size();
        int maxStackSize = itemToAdd.getMaxStackSize();

        // How many new stacks are needed for the leftover amount
        int neededStacks = (int) Math.ceil(amountToAdd / (double) maxStackSize);

        return neededStacks <= freeSlots;
    }

    public boolean addToStash(ItemStack itemToAdd) {
        if (!canAddToStash(itemToAdd)) {
            return false;
        }

        int amountToAdd = itemToAdd.getAmount();

        // Try to merge with existing stacks
        for (ItemStack stack : stash) {
            if (stack != null && stack.isSimilar(itemToAdd)) {
                int maxStackSize = stack.getMaxStackSize();
                int freeSpace = maxStackSize - stack.getAmount();

                if (freeSpace > 0) {
                    int adding = Math.min(amountToAdd, freeSpace);
                    stack.setAmount(stack.getAmount() + adding);
                    amountToAdd -= adding;
                    if (amountToAdd <= 0) return true;
                }
            }
        }

        // Add new stacks for the remaining items
        while (amountToAdd > 0) {
            int addAmount = Math.min(amountToAdd, itemToAdd.getMaxStackSize());
            ItemStack newStack = itemToAdd.clone();
            newStack.setAmount(addAmount);
            stash.add(newStack);
            amountToAdd -= addAmount;
        }

        return true;
    }

}
