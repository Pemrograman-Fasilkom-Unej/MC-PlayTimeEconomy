package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.OnPlaceBlock;
import com.darkun7.timerald.item.items.ClockworkBomb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlockPlaceListener implements Listener {

    private final List<OnPlaceBlock> registeredPlaceItems = new ArrayList<>();

    public BlockPlaceListener(Timerald plugin) {
        // Register all OnPlaceBlock-based items here
        registeredPlaceItems.add(new ClockworkBomb(plugin));
        // Add more block-placing items in the future
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (item == null) return;

        for (OnPlaceBlock customItem : registeredPlaceItems) {
            if (customItem.matches(item)) {
                customItem.onPlace(player, event);
                break; // Stop if we found a match
            }
        }
    }
}
