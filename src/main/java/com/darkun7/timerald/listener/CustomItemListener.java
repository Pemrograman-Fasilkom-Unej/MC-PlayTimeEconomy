package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.ConsumableItemHandler;
import com.darkun7.timerald.item.items.*;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CustomItemListener implements Listener {

    private final List<ConsumableItemHandler> consumables = new ArrayList<>();

    public CustomItemListener(Timerald plugin) {
        consumables.add(new TimeElixir(plugin));
        // later: consumables.add(new AnotherItem(...));
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        for (ConsumableItemHandler handler : consumables) {
            if (handler.matches(item)) {
                handler.onConsume(player, item);
                break;
            }
        }
    }
}
