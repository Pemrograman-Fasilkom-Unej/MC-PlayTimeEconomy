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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NymphSnack implements ConsumableItemHandler {

    private final Timerald plugin;
    private final List<PotionEffect> effects;


    public NymphSnack(Timerald plugin) {
        this.plugin = plugin;
        // Load effects
        this.effects = new ArrayList<>();
        ConfigurationSection snack = plugin.getConfig().getConfigurationSection("shop.nymph-snack");

        for (String line : snack.getStringList("effects")) {
            try {
                String[] parts = line.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                int duration = Integer.parseInt(parts[1]) * 20;
                int amplifier = Integer.parseInt(parts[2]) - 1;
                if (type != null) {
                    effects.add(new PotionEffect(type, duration, amplifier, false, false));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid effect format: " + line);
            }
        }
    }

    @Override
    public boolean matches(ItemStack item) {
        ConfigurationSection snack = plugin.getConfig().getConfigurationSection("shop.nymph-snack");
        if (snack == null || item == null || !item.hasItemMeta()) return false;

        String name = snack.getString("name");
        String materialStr = snack.getString("material", "DRIED_KELP");
        List<String> lore = snack.getStringList("lore");

        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (item.getType() != material) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && name.equals(meta.getDisplayName()) && lore.equals(meta.getLore());
    }

    @Override
    public void onConsume(Player player, ItemStack item) {
        ConfigurationSection snack = plugin.getConfig().getConfigurationSection("shop.nymph-snack");
        int minutes = snack.getInt("minutes", 20);
        String name = snack.getString("name");

        UUID uuid = player.getUniqueId();
        PlayTimeLimiterAPI api = PlayTimeLimiter.getInstance().getAPI();
        api.reduceDailyUsed(uuid, minutes);

        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.min(player.getHealth() + 3.0, maxHealth));
        player.setFoodLevel(Math.min(player.getFoodLevel() + 3, 20));
        player.setSaturation(Math.min(player.getSaturation() + 2, 20));

        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        player.sendMessage("§aYou consumed a " + name +".");
    }
}
