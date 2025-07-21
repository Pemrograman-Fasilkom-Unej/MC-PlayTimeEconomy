package com.darkun7.timerald.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;

public interface OnUseItem {
    boolean matches(ItemStack item);
    void onUse(Player player, ItemStack item, PlayerInteractEvent event);
}
