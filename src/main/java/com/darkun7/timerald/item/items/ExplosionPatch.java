package com.darkun7.timerald.item.items;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.OnUseItem;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class ExplosionPatch implements OnUseItem {

    private final String displayName;
    private final Material material;
    private final List<String> lore;
    private final Timerald plugin;

    public ExplosionPatch(Timerald plugin) {
        this.plugin = plugin;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop.explosion-patch");
        if (section == null) {
            throw new IllegalStateException("Missing 'shop.explosion-patch' config section.");
        }

        this.displayName = section.getString("name", "§f<§7Explosion §8Patch§f>");
        this.material = Material.getMaterial(section.getString("material", "ARMS_UP_POTTERY_SHERD").toUpperCase());
        this.lore = section.getStringList("lore");
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.hasDisplayName()
                && meta.getDisplayName().equals(displayName)
                && meta.hasLore()
                && meta.getLore().equals(lore);
    }

    @Override
    public void onUse(Player player, ItemStack item, PlayerInteractEvent event) {
        Location playerLoc = player.getLocation();
        Block below = playerLoc.clone().subtract(0, 1, 0).getBlock();
        boolean useSand = below.getType() == Material.SAND;

        Material baseMaterial = useSand ? Material.SAND : Material.DIRT;
        Material topMaterial = useSand ? Material.SAND : Material.GRASS_BLOCK;

        // Direction player is facing
        Vector dir = playerLoc.getDirection().normalize();

        // Center of fill area (a few blocks in front of player)
        Location center = playerLoc.clone().add(dir.multiply(2));

        int radius = 4;
        int height = 3;

        // Iterate through a 3D radial area
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.sqrt(x * x + z * z) > radius) continue; // outside circle

                for (int y = 0; y < height-1; y++) {
                    Location blockLoc = center.clone().add(x, y-1, z);
                    Block block = blockLoc.getBlock();

                    if (block.getType() == Material.AIR) {
                        // If it's the topmost layer, use grass (if applicable)
                        if (y == height - 2 && !useSand) {
                            block.setType(topMaterial);
                        } else {
                            block.setType(baseMaterial);
                        }
                    }
                }
            }
        }

        // Consume one item
        item.setAmount(item.getAmount() - 1);
    }
}
