package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.OnUseItem;
import com.darkun7.timerald.item.items.SmokeBomb;
import com.darkun7.timerald.item.items.EscapeFeather;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

public class ItemUseListener implements Listener {

    private final List<OnUseItem> registeredItems = new ArrayList<>();

    public ItemUseListener(Timerald plugin) {
        // Register custom items
        registeredItems.add(new SmokeBomb(plugin));
        registeredItems.add(new EscapeFeather(plugin));
        // Add more items here later
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        for (OnUseItem customItem : registeredItems) {
            if (customItem.matches(item)) {
                customItem.onUse(player, item, event);
                event.setCancelled(true); // optional
                break;
            }
        }
    }
}
