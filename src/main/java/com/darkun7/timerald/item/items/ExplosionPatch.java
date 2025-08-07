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
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;

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
        event.setCancelled(true);
        Location playerLoc = player.getLocation();
        Block below = playerLoc.clone().subtract(0, 1, 0).getBlock();

        // Detect if the player is in the Nether
        boolean isNether = playerLoc.getWorld().getEnvironment() == Environment.NETHER;
        boolean useSand = !isNether && below.getType() == Material.SAND;
        Biome biome = playerLoc.getWorld().getBiome(playerLoc);

        Material baseMaterial;
        Material topMaterial;

        if (isNether) {
            baseMaterial = Material.NETHERRACK;
            topMaterial = Material.NETHERRACK;
        } else if (useSand) {
            baseMaterial = Material.SAND;
            topMaterial = Material.SAND;
        } else if (biome == Biome.MUSHROOM_FIELDS) {
            baseMaterial = Material.DIRT;
            topMaterial = Material.MYCELIUM;
        } else {
            baseMaterial = Material.DIRT;
            topMaterial = Material.GRASS_BLOCK;
        }

        // Direction player is facing
        Vector dir = playerLoc.getDirection().normalize();

        // Center of fill area (a few blocks in front of player)
        Location center = playerLoc.clone().add(dir.multiply(2));
        int baseY = playerLoc.getBlockY()-1; // Use player height as reference

        int radius = 4;
        int depth = 4; // Increased depth

        // Blocks to replace (not just air)
        List<Material> replaceable = List.of(
                Material.AIR,
                Material.SNOW,
                Material.SNOW_BLOCK,
                Material.TALL_GRASS,
                Material.SHORT_GRASS,
                Material.FERN,
                Material.LARGE_FERN,
                Material.DEAD_BUSH,
                Material.FIRE,
                Material.PINK_PETALS,
                Material.WILDFLOWERS,
                Material.LEAF_LITTER,
                Material.MOSS_CARPET 
        );

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.sqrt(x * x + z * z) > radius) continue;

                for (int y = 0; y > -depth; y--) {
                    Location blockLoc = new Location(center.getWorld(), center.getX() + x, baseY + y, center.getZ() + z);
                    Block block = blockLoc.getBlock();

                    if (replaceable.contains(block.getType()) || block.isPassable()) {
                        if (y == 0) {
                            block.setType(topMaterial); // Top layer
                        } else {
                            block.setType(baseMaterial); // Lower layers
                        }
                    }
                }
            }
        }

        // Consume one item
        item.setAmount(item.getAmount() - 1);
    }

}
