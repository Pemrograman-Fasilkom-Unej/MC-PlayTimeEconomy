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

import java.util.ArrayList;
import java.util.List;

public class SmokeBomb implements OnUseItem {

    private final String displayName;
    private final Material material;
    private final List<String> lore;
    private final List<PotionEffect> effects;
    private final Particle particle;
    private final int particleAmount;
    private final Timerald plugin;

    public SmokeBomb(Timerald plugin) {
        this.plugin = plugin;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop.smoke-bomb");
        if (section == null) {
            throw new IllegalStateException("Missing 'shop.smoke-bomb' config section.");
        }

        this.displayName = section.getString("name", "§f<§7Smoke §8Bomb§f>");
        this.material = Material.getMaterial(section.getString("material", "FIREWORK_STAR").toUpperCase());
        this.lore = section.getStringList("lore");

        // Load effects
        this.effects = new ArrayList<>();
        for (String line : section.getStringList("effects")) {
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

        // Optional particle
        String particleType = section.getString("particles.type", "LARGE_SMOKE");
        this.particle = Particle.valueOf(particleType.toUpperCase());
        this.particleAmount = section.getInt("particles.amount", 20);
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
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        if (particle != null) {
            player.getWorld().spawnParticle(particle, player.getLocation(), particleAmount, 0.5, 1, 0.5, 0.01);
        }

        // Consume one item
        item.setAmount(item.getAmount() - 1);
    }
}
