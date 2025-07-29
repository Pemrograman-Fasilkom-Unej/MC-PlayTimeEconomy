package com.darkun7.timerald.item.items;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.OnUseItem;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.World.Environment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class EscapeFeather implements OnUseItem {

    private final Timerald plugin;
    private final String displayName;
    private final Material material;
    private final List<String> lore;

    public EscapeFeather(Timerald plugin) {
        this.plugin = plugin;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop.escape-feather");
        if (section == null) {
            throw new IllegalStateException("Missing 'shop.escape-feather' config section.");
        }

        this.displayName = section.getString("name", "&f<&6Escape &eFeather&f>");
        this.material = Material.getMaterial(section.getString("material", "FEATHER").toUpperCase());
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
        Environment env = player.getWorld().getEnvironment();
        if (env != Environment.NETHER && env != Environment.THE_END) {
            player.sendMessage("§cYou can only use the " + displayName + " §cin the §4Nether §eand §dEnd§c.");
            return;
        }

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 50, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1f);

        // 🏠 Destination
        Location bed = player.getBedSpawnLocation();
        if (bed == null) {
            World overworld = Bukkit.getWorld("world"); // "world" is the default Overworld name
            if (overworld != null) {
                bed = overworld.getSpawnLocation();
            } else {
                bed = player.getWorld().getSpawnLocation(); // fallback
            }
        }
        
        final Location teleportLocation = bed;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = teleportLocation.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.END_ROD, teleportLocation.clone().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.05);
                world.playSound(teleportLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
            }
            player.teleport(teleportLocation);
            player.sendMessage("§aYou used a " + displayName + "§a and returned to your spawn point.");
        }, 10L);

        // Consume one item
        item.setAmount(item.getAmount() - 1);
    }
}
