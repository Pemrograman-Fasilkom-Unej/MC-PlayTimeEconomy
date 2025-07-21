package com.darkun7.timerald.item;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.darkun7.timerald.Timerald;

import java.util.List;

public class PlayerHeadEffect implements TickItemEffect {

    private final Timerald plugin;

    public PlayerHeadEffect(Timerald plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isActive(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.PLAYER_HEAD) return false;

        ItemMeta meta = helmet.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String expected = "§f" + player.getName() + "'s Head";
        return expected.equals(meta.getDisplayName());
    }

    @Override
    public void apply(Player player) {
        List<String> effects = plugin.getConfig().getStringList("shop.playerhead.effects");

        for (String line : effects) {
            try {
                String[] parts = line.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                int duration = Integer.parseInt(parts[1]) * 20;
                int amplifier = Integer.parseInt(parts[2]) - 1;

                if (type != null && !player.hasPotionEffect(type)) {
                    player.addPotionEffect(new PotionEffect(type, duration, amplifier, false, false));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid effect format: " + line);
            }
        }
    }

    @Override
    public void remove(Player player) {
        List<String> effects = plugin.getConfig().getStringList("shop.playerhead.effects");

        for (String line : effects) {
            try {
                String[] parts = line.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());

                if (type != null) {
                    player.removePotionEffect(type);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing effect: " + line);
            }
        }
    }
}
