package com.darkun7.timerald.item.items;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.OnPlaceBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ClockworkBomb implements OnPlaceBlock {

    private final Timerald plugin;
    private final String displayName;
    private final Material material;
    private final List<String> lore;

    public ClockworkBomb(Timerald plugin) {
        this.plugin = plugin;

        var section = plugin.getConfig().getConfigurationSection("shop.clockwork-bomb");
        if (section == null) {
            throw new IllegalStateException("Missing 'shop.clockwork-bomb' config section.");
        }

        this.displayName = section.getString("name", "§f<§6Clockwork §7Bomb§f>");
        this.material = Material.getMaterial(section.getString("material", "TNT").toUpperCase());
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
    public void onPlace(Player player, BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Location location = block.getLocation();

        // Remove placed TNT block
        block.setType(Material.AIR);

        TNTPrimed tnt = block.getWorld().spawn(location.add(0.5, 0, 0.5), TNTPrimed.class);
        tnt.setFuseTicks(100); // 5 seconds
        tnt.setGravity(false); // Prevent falling
        tnt.setVelocity(new org.bukkit.util.Vector(0, 0, 0)); // Ensure no movement

        // After fuse, custom explosion
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tnt.remove(); // Cancel default TNT explosion
            location.getWorld().createExplosion(location, 0F, false, false);

            destroyNearbyBlocks(player, location, 5); // radius 5 (radial)
        }, 20L * 5); // 5 seconds
    }

    private void destroyNearbyBlocks(Player player, Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block target = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z
                    );

                    double distance = center.distance(target.getLocation().add(0.5, 0.5, 0.5));
                    if (distance > radius) continue; // ✅ Radial check

                    if (!isOre(target.getType()) && canBreak(target.getType())) {
                        if (!shouldDrop(target.getType())) {
                            // No drop: just remove block
                            target.setType(Material.AIR);
                        } else {
                            // Drop items naturally like player break
                            target.getDrops(player.getInventory().getItemInMainHand(), player)
                                    .forEach(drop -> target.getWorld().dropItemNaturally(target.getLocation(), drop));
                            target.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    private boolean canBreak(Material material) {
        // Prevent unbreakable blocks
        return switch (material) {
            case AIR, BEDROCK, BARRIER, END_PORTAL_FRAME, END_PORTAL, NETHER_PORTAL, COMMAND_BLOCK,
                 REPEATING_COMMAND_BLOCK, CHAIN_COMMAND_BLOCK -> false;
            default -> true;
        };
    }

    private boolean isOre(Material material) {
        return switch (material) {
            // Overworld Ores
            case COAL_ORE, IRON_ORE, GOLD_ORE, DIAMOND_ORE, EMERALD_ORE,
                REDSTONE_ORE, LAPIS_ORE, COPPER_ORE,
                DEEPSLATE_COAL_ORE, DEEPSLATE_IRON_ORE, DEEPSLATE_GOLD_ORE,
                DEEPSLATE_DIAMOND_ORE, DEEPSLATE_EMERALD_ORE, DEEPSLATE_REDSTONE_ORE,
                DEEPSLATE_LAPIS_ORE, DEEPSLATE_COPPER_ORE,

            // Nether Valuable Blocks
            NETHER_QUARTZ_ORE, ANCIENT_DEBRIS,

            // Amethyst-related blocks
            AMETHYST_BLOCK, BUDDING_AMETHYST, SMALL_AMETHYST_BUD, MEDIUM_AMETHYST_BUD,
            LARGE_AMETHYST_BUD, AMETHYST_CLUSTER -> true;

            default -> false;
        };
    }


    private boolean shouldDrop(Material material) {
        return switch (material) {
            case STONE, COBBLESTONE, DIRT, GRASS_BLOCK, SAND, RED_SAND,
                GRAVEL, NETHERRACK, END_STONE -> false; // No drops
            default -> true;
        };
    }

}
