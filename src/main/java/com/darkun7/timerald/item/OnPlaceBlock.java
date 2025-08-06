package com.darkun7.timerald.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.BlockPlaceEvent;

public interface OnPlaceBlock {
    boolean matches(ItemStack item);
    void onPlace(Player player, BlockPlaceEvent event);
}