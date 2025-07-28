package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.BlockFace;
import org.bukkit.Bukkit;
import org.bukkit.event.block.BlockIgniteEvent;

import java.util.Arrays;
import java.util.List;

public class PreventPlacementListener implements Listener {

    private final Timerald plugin;

    public PreventPlacementListener(Timerald plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        String placedBlockType = event.getBlockPlaced().getType().toString();

        String displayName = null;
        if (main != null && main.hasItemMeta() && main.getItemMeta().hasDisplayName()) {
            displayName = main.getItemMeta().getDisplayName();
        } else if (off != null && off.hasItemMeta() && off.getItemMeta().hasDisplayName()) {
            displayName = off.getItemMeta().getDisplayName();
        }

        // Read from config
        ConfigurationSection lamp = plugin.getConfig().getConfigurationSection("shop.luminous-lantern");
        String luminousLantern = lamp.getString("name");

        ConfigurationSection playerhead = plugin.getConfig().getConfigurationSection("shop.playerhead");
        String head = "§f" + player.getName() + "'s Head";
        List<String> headTypes = Arrays.asList(
            "PLAYER_HEAD",
            "PLAYER_WALL_HEAD"
        );

        List<String> customItem = List.of(
            luminousLantern,
            head
        );

        if (displayName == null || !customItem.contains(displayName)) return;
        
        // Head
        if (off.hasItemMeta() && head.equals(off.getItemMeta().getDisplayName())) {
            if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LUCK) && headTypes.contains(placedBlockType)) {
                event.setCancelled(true);
            }
        }

        // Lantern
        if (displayName.equals(luminousLantern)) {
            Block placedBlock = event.getBlock();
            Block clicked = event.getBlockAgainst();
            BlockFace face = getBlockFace(placedBlock, clicked);
            if (face == null) return;

            Block target = clicked.getRelative(face);
            Block below = target.getRelative(BlockFace.DOWN);

            if (placedBlockType.equals(lamp.getString("material")) 
                && (below.getType().isSolid() || below.getType().isFlammable())) {
                event.setCancelled(true);
                BlockIgniteEvent igniteEvent = new BlockIgniteEvent(
                    target, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, player
                );
                Bukkit.getPluginManager().callEvent(igniteEvent);

                if (!igniteEvent.isCancelled()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (below.getType() == Material.SOUL_SOIL || below.getType() == Material.SOUL_SAND) {
                            placedBlock.setType(Material.SOUL_FIRE);
                        } else {
                            target.setType(Material.FIRE);
                        }
                    });
                    player.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 1.0f);
                } else {
                    player.sendMessage("§cFire ignition was blocked by another plugin.");
                }
            }
        }

        
    }

    private BlockFace getBlockFace(Block placed, Block against) {
        for (BlockFace face : BlockFace.values()) {
            if (against.getRelative(face).getLocation().equals(placed.getLocation())) {
                return face;
            }
        }
        return null;
    }
}
