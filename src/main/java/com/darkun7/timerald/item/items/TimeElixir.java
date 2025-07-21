package com.darkun7.timerald.item.items;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.ConsumableItemHandler;
import com.darkun7.limiter.PlayTimeLimiter;
import com.darkun7.limiter.api.PlayTimeLimiterAPI;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class TimeElixir implements ConsumableItemHandler {

    private final Timerald plugin;

    public TimeElixir(Timerald plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean matches(ItemStack item) {
        ConfigurationSection elixir = plugin.getConfig().getConfigurationSection("shop.time-elixir");
        if (elixir == null || item == null || !item.hasItemMeta()) return false;

        String name = elixir.getString("name");
        String materialStr = elixir.getString("material", "POTION");
        List<String> lore = elixir.getStringList("lore");

        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (item.getType() != material) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && name.equals(meta.getDisplayName()) && lore.equals(meta.getLore());
    }

    @Override
    public void onConsume(Player player, ItemStack item) {
        ConfigurationSection elixir = plugin.getConfig().getConfigurationSection("shop.time-elixir");
        int minutes = elixir.getInt("minutes", 20);
        String name = elixir.getString("name");

        UUID uuid = player.getUniqueId();
        PlayTimeLimiterAPI api = PlayTimeLimiter.getInstance().getAPI();
        api.reduceDailyUsed(uuid, minutes);

        // Heal + Hunger
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.min(player.getHealth() + 6.0, maxHealth));
        player.setFoodLevel(Math.min(player.getFoodLevel() + 6, 20));
        player.setSaturation(Math.min(player.getSaturation() + 4, 20));

        player.sendMessage("§aYou consumed a " + name + " and reduced §f" + minutes + " minute(s) §aof playtime.");
    }
}
