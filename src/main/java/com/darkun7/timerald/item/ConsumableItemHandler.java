package com.darkun7.timerald.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ConsumableItemHandler {
    boolean matches(ItemStack item);
    void onConsume(Player player, ItemStack item);
}
