package com.darkun7.timerald.item;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.darkun7.timerald.Timerald;

import java.util.*;

public class LuminousLanternEffect implements TickItemEffect {

    private final Timerald plugin;
    private final Map<UUID, Location> previousLight = new HashMap<>();

    public LuminousLanternEffect(Timerald plugin) {
        this.plugin = plugin;
    }

    private boolean isLantern(ItemStack item) {
        if (item == null) return false;

        if (item.getType() != Material.SOUL_LANTERN) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        ConfigurationSection lantern = plugin.getConfig().getConfigurationSection("shop.luminous-lantern");
        if (lantern == null) return false;

        String expected = lantern.getString("name");
        return expected != null && expected.equals(meta.getDisplayName());
    }

    @Override
    public boolean isActive(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        return isLantern(main) || isLantern(off);
    }

    @Override
    public void apply(Player player) {
        // Apply potion effects
        List<String> effects = plugin.getConfig().getStringList("shop.luminous-lantern.effects");
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

        // Dynamic light block handling
        UUID uuid = player.getUniqueId();
        Location baseLoc = player.getLocation().getBlock().getLocation();

        Location previousLoc = previousLight.get(uuid);
        if (previousLoc != null && !previousLoc.equals(baseLoc)) {
            Block previousBlock = previousLoc.getBlock();
            if (previousBlock.getType() == Material.LIGHT) {
                previousBlock.setType(Material.AIR);
            }
        }

        // Try to find a valid location around the player to place the light
        Location targetLoc = null;
        int range = 2; // You can increase this if needed
        for (int yOffset = 0; yOffset <= range; yOffset++) {
            Location up = baseLoc.clone().add(0, yOffset, 0);
            Location down = baseLoc.clone().add(0, -yOffset, 0);

            if (up.getBlock().getType() == Material.AIR || up.getBlock().getType() == Material.LIGHT) {
                targetLoc = up;
                break;
            }

            if (down.getBlock().getType() == Material.AIR || down.getBlock().getType() == Material.LIGHT) {
                targetLoc = down;
                break;
            }
        }

        if (targetLoc != null) {
            Block currentBlock = targetLoc.getBlock();
            currentBlock.setType(Material.LIGHT);
            currentBlock.setBlockData(plugin.getServer().createBlockData("minecraft:light[level=15]"), false);
            previousLight.put(uuid, targetLoc);
        }
    }

    @Override
    public void remove(Player player) {
        // Remove potion effects
        List<String> effects = plugin.getConfig().getStringList("shop.luminous-lantern.effects");
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

        // Remove any lingering light block
        UUID uuid = player.getUniqueId();
        Location last = previousLight.remove(uuid);
        if (last != null) {
            Block block = last.getBlock();
            if (block.getType() == Material.LIGHT) {
                block.setType(Material.AIR);
            }
        }
    }
}
