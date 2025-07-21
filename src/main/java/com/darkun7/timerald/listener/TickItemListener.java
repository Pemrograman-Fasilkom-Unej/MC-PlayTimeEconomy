package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.TickItemEffect;
import com.darkun7.timerald.item.PlayerHeadEffect;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TickItemListener implements Listener {

    private final Timerald plugin;
    private final List<TickItemEffect> tickEffects = new ArrayList<>();
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();

    public TickItemListener(Timerald plugin) {
        this.plugin = plugin;

        // Register passive tick effects
        tickEffects.add(new PlayerHeadEffect(plugin));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        startTask(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopTask(event.getPlayer());
    }

    private void startTask(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                for (TickItemEffect effect : tickEffects) {
                    if (effect.isActive(player)) {
                        effect.apply(player);
                    } else {
                        effect.remove(player);
                    }
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 10L); // every 0.5s
        tasks.put(uuid, task);
    }

    private void stopTask(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) {
            tasks.get(uuid).cancel();
            tasks.remove(uuid);
        }

        // Cleanup effects
        for (TickItemEffect effect : tickEffects) {
            effect.remove(player);
        }
    }
}
