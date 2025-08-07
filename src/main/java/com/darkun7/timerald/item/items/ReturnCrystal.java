package com.darkun7.timerald.item.items;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.OnUseItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReturnCrystal implements OnUseItem {

    private final Timerald plugin;
    private final String displayName;
    private final Material material;
    private final List<String> baseLore;
    private final Random random = new Random();

    public ReturnCrystal(Timerald plugin) {
        this.plugin = plugin;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop.return-crystal");
        if (section == null) {
            throw new IllegalStateException("Missing 'shop.return-crystal' config section.");
        }

        this.displayName = section.getString("name", "§f<§dReturn §9Crystal§f>");
        this.material = Material.getMaterial(section.getString("material", "AMETHYST_SHARD").toUpperCase());
        this.baseLore = section.getStringList("lore");
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        if (item.getType() != material) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.hasLore()) return false;

        List<String> itemLore = meta.getLore();
        return itemLore.size() > 1 && baseLore.size() > 1 && itemLore.get(1).startsWith(baseLore.get(1));
    }

    @Override
    public void onUse(Player player, ItemStack item, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;

        List<String> itemLore = new ArrayList<>(meta.getLore());
        while (itemLore.size() < 5) itemLore.add("");

        if (player.isSneaking()) {
            // Teleport logic
            try {
                String worldName = ChatColor.stripColor(itemLore.get(2)).replace("World:", "").trim();
                String[] coords = ChatColor.stripColor(itemLore.get(3))
                        .replace("X:", "")
                        .replace("Y:", ",")
                        .replace("Z:", ",")
                        .split(",");

                int x = Integer.parseInt(coords[0].trim());
                int y = Integer.parseInt(coords[1].trim());
                int z = Integer.parseInt(coords[2].trim());

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    player.sendMessage("§cInvalid world in lore.");
                    return;
                }

                Location baseLoc = new Location(world, x, y, z);

                Location safeLoc = null;
                int attempts = 15;
                int radius = 5;

                for (int i = 0; i < attempts; i++) {
                    int offsetX = random.nextInt(radius * 2 + 1) - radius;
                    int offsetZ = random.nextInt(radius * 2 + 1) - radius;

                    int checkX = baseLoc.getBlockX() + offsetX;
                    int checkZ = baseLoc.getBlockZ() + offsetZ;
                    int checkY = baseLoc.getBlockY(); // Optional: can try varying Y too if needed

                    safeLoc = getSafeTeleportLocation(world, checkX, checkY, checkZ);
                    if (safeLoc != null) break;
                }

                if (safeLoc == null) {
                    player.sendMessage("§cCould not find a safe teleport location!");
                    return;
                }


                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 50, 0.3, 0.3, 0.3, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1f);
                final Location teleportLocation = safeLoc;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.teleport(teleportLocation);
                    player.sendMessage("§aTeleported using " + displayName);
                }, 10L);

                // Usage tracking
                String usesLine = ChatColor.stripColor(itemLore.get(4)).replace("[", "").replace("]", "");
                String[] uses = usesLine.split("/");
                int current = Integer.parseInt(uses[0].trim());
                int max = Integer.parseInt(uses[1].trim());

                current--;
                if (current <= 0) {
                    player.getInventory().remove(item);
                    player.sendMessage("§eThe " + displayName + "§e has shattered!");
                    return;
                } else {
                    itemLore.set(4, "§f[" + current + "/" + max + "]");
                    meta.setLore(itemLore);
                    item.setItemMeta(meta);
                }

            } catch (Exception e) {
                player.sendMessage("§cFailed to teleport. Invalid item data.");
            }

        } else {
            // Marking logic
            Block block = event.getClickedBlock();
            if (block == null) {
                player.sendMessage("§7You must sneak to teleport.");
                return;
            }

            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                // player.sendMessage("§cRight click a block to mark.");
                return;
            }

            if (block.getType() != Material.LODESTONE) {
                player.sendMessage("§cYou must right-click a Lodestone to mark position.");
                return;
            }

            Location loc = block.getLocation();
            String worldLine = "§eWorld:" + loc.getWorld().getName();
            String coordLine = "§eX:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ();

            itemLore.set(0, "§7Right click while sneaking to use");
            itemLore.set(2, worldLine);
            itemLore.set(3, coordLine);

            if (ChatColor.stripColor(itemLore.get(4)).isEmpty() || !itemLore.get(4).matches(".*\\[\\d+/\\d+\\].*")) {
                itemLore.set(4, "§f[3/3]");
            }

            meta.setLore(itemLore);
            item.setItemMeta(meta);

            player.sendMessage(displayName + "§a is now linked to this Lodestone.");
            player.sendMessage("§aSneak + Right Click to return here.");
        }
    }

    private Location getSafeTeleportLocation(World world, int x, int y, int z) {
        for (int dy = 3; dy >= -3; dy--) {
            int checkY = y + dy;
            if (checkY < world.getMinHeight() || checkY >= world.getMaxHeight()) continue;

            Block block = world.getBlockAt(x, checkY, z);
            Block above = world.getBlockAt(x, checkY + 1, z);
            Block above2 = world.getBlockAt(x, checkY + 2, z);

            if (block.getType().isSolid()
                    && above.getType() == Material.AIR
                    && above2.getType() == Material.AIR
                    && block.getType() != Material.LAVA
                    && block.getType() != Material.WATER) {
                return new Location(world, x + 0.5, checkY + 1, z + 0.5);
            }
        }
        return null;
    }
}
