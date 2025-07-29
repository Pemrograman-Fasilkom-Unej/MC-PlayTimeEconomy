package com.darkun7.timerald.data;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class CustomItemManager {
    private static final Map<String, ItemStack> items = new HashMap<>();

    public static void load(FileConfiguration config) {
        items.clear();
        if (!config.contains("shop")) return;

        for (String key : config.getConfigurationSection("shop").getKeys(false)) {
            String path = "shop." + key;
            String matName = config.getString(path + ".material");
            if (matName == null || matName.isEmpty()) {
                Bukkit.getLogger().warning("[CustomItemManager] Missing material for: " + key);
                continue;
            }

            Material material;
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                Bukkit.getLogger().warning("[CustomItemManager] Invalid material '" + matName + "' for item: " + key);
                continue;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            // Name & Lore
            if (config.contains(path + ".name")) {
                meta.setDisplayName(config.getString(path + ".name"));
            }
            if (config.contains(path + ".lore")) {
                meta.setLore(config.getStringList(path + ".lore"));
            }

            // Glowing effect
            if (config.getBoolean(path + ".enchanted", false)) {
                meta.addEnchant(Enchantment.INFINITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.addUnsafeEnchantment(Enchantment.INFINITY, 1);
            }

            item.setItemMeta(meta);
            items.put(key, item);
        }

        Bukkit.getLogger().info("[CustomItemManager] Loaded " + items.size() + " custom items.");
    }


    public static ItemStack getItem(String key) {
        return items.get(key);
    }

    public static boolean matches(ItemStack item, String idOrMaterialName) {
        if (idOrMaterialName == null || idOrMaterialName.equalsIgnoreCase("AIR")) {
            return item == null || item.getType() == Material.AIR;
        }

        // Split material and variation (e.g. "POTION:THICK")
        String[] parts = idOrMaterialName.split(":");
        String base = parts[0];

        // Check for custom item first
        ItemStack expected = items.get(base);
        if (expected != null) {
            return matchesCustomItem(item, expected);
        }

        // Check for vanilla material
        try {
            Material mat = Material.valueOf(base.toUpperCase());
            if (item == null || item.getType() != mat) return false;

            // Potion variant check
            if (mat == Material.POTION && parts.length > 1) {
                String variant = parts[1].toUpperCase();
                if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;

                PotionType type;
                try {
                    type = PotionType.valueOf(variant);
                } catch (IllegalArgumentException e) {
                    return false;
                }

                return meta.getBasePotionData().getType() == type;
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean matchesCustomItem(ItemStack item, ItemStack expected) {
        if (item == null || item.getType() != expected.getType()) return false;
        if (!item.hasItemMeta() || !expected.hasItemMeta()) return false;

        ItemMeta actualMeta = item.getItemMeta();
        ItemMeta expectedMeta = expected.getItemMeta();

        if (!Objects.equals(actualMeta.getDisplayName(), expectedMeta.getDisplayName())) return false;
        if (actualMeta.hasLore() && expectedMeta.hasLore()) {
            return Objects.equals(actualMeta.getLore(), expectedMeta.getLore());
        }

        return !actualMeta.hasLore() && !expectedMeta.hasLore();
    }


}
