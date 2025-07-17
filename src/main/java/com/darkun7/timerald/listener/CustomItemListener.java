package com.darkun7.timerald.listener;

import com.darkun7.limiter.PlayTimeLimiter;
import com.darkun7.limiter.api.PlayTimeLimiterAPI;
import com.darkun7.timerald.Timerald;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class CustomItemListener implements Listener {

    private final Timerald plugin;

    public CustomItemListener(Timerald plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("time-elixir");
        if (section == null) return;

        String name = section.getString("name");
        String materialStr = section.getString("material", "POTION");
        int minutes = section.getInt("minutes", 20);
        List<String> lore = section.getStringList("lore");

        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (material == null || item == null || item.getType() != material || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().equals(name)) return;
        if (!meta.hasLore() || !meta.getLore().equals(lore)) return;

        // Apply effect
        UUID uuid = player.getUniqueId();
        PlayTimeLimiterAPI api = PlayTimeLimiter.getInstance().getAPI();
        api.reduceDailyUsed(uuid, minutes);

        // Apply effect: Restore health
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.min(player.getHealth() + 6.0, maxHealth)); // Heal 3 hearts

        // Apply effect: Restore hunger
        player.setFoodLevel(Math.min(player.getFoodLevel() + 6, 20)); // +3 bars
        player.setSaturation(Math.min(player.getSaturation() + 4, 20)); // Extra satiety

        player.sendMessage("§aYou consumed a Time Elixir and reduced §f" + minutes + " minute(s) §aof playtime.");
    }
}
