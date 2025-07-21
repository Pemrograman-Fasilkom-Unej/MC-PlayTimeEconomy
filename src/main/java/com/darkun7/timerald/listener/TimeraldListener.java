package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.TimeraldManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;
import java.util.Random;

public class TimeraldListener implements Listener {

    private final Timerald plugin;
    private final Random random = new Random();

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

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        ItemStack offHand = killer.getInventory().getItemInOffHand();
        if (offHand == null || offHand.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = offHand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String expected = "§f" + killer.getName() + "'s Head";
        if (!meta.getDisplayName().equals(expected)) return;

        double chance = plugin.getConfig().getDouble("shop.playerhead.emerald_chance", 5.0);
        if (killer.hasPotionEffect(org.bukkit.potion.PotionEffectType.LUCK)) {
            chance = plugin.getConfig().getDouble("shop.playerhead.luck_boosted_chance", 50.0);
        }

        if (random.nextDouble() * 100 < chance) {
            event.getDrops().add(new ItemStack(Material.EMERALD, 1));
        }
    }
}
