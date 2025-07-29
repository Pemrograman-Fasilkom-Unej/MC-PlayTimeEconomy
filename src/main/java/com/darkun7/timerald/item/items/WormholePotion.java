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

import org.bukkit.*;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.World.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class WormholePotion implements ConsumableItemHandler, Listener {

    private final Timerald plugin;
    private final String displayName;
    private final List<PotionEffect> effects;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public WormholePotion(Timerald plugin) {
        this.plugin = plugin;
        
        this.effects = new ArrayList<>();
        ConfigurationSection consumable = plugin.getConfig().getConfigurationSection("shop.wormhole-potion");
        this.displayName = consumable.getString("name","§f<§bWormhole §9potion§f>");

        for (String line : consumable.getStringList("effects")) {
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
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean matches(ItemStack item) {
        ConfigurationSection consumable = plugin.getConfig().getConfigurationSection("shop.wormhole-potion");
        if (consumable == null || item == null || !item.hasItemMeta()) return false;

        String name = consumable.getString("name");
        String materialStr = consumable.getString("material", "POTION");
        List<String> lore = consumable.getStringList("lore");

        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (item.getType() != material) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && name.equals(meta.getDisplayName()) && lore.equals(meta.getLore());
    }

    @Override
    public void onConsume(Player player, ItemStack item) {
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.min(player.getHealth() + 6.0, maxHealth));
        player.setFoodLevel(Math.min(player.getFoodLevel() + 7, 20));
        player.setSaturation(Math.min(player.getSaturation() + 5, 20));

        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        Environment env = player.getWorld().getEnvironment();

        // ✅ Save the player's current location
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());

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

        player.sendMessage("§aYou consumed a " + displayName +"!\nReturn to previous location by interact with §fBell§a.");
    }

    @EventHandler
    public void onBellInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BELL) {
            UUID uuid = player.getUniqueId();
            if (lastLocations.containsKey(uuid)) {
                Location lastLocation = lastLocations.remove(uuid); // remove after use
                player.teleport(lastLocation);
                player.sendMessage("§aYou have been returned to your previous location!");
            }
        }
    }

}
