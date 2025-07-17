package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.TimeraldManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TimeraldListener implements Listener {

    private final Timerald plugin;

    public TimeraldListener(Timerald plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        UUID uuid = player.getUniqueId();
        TimeraldManager manager = plugin.getTimeraldManager();

        if (item.getType() == Material.EMERALD) {
            int worth = item.getAmount() * 1;
            manager.add(uuid, worth);
            player.getInventory().setItemInMainHand(null);
            player.sendMessage("§aDeposited " + worth + " Timerald.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TimeraldManager manager = plugin.getTimeraldManager();
        manager.registerAlias(player.getUniqueId(), player.getName());
    }
}
