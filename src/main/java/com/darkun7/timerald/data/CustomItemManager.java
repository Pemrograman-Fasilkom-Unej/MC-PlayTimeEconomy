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
            if (key == "playtime") continue;
            String path = "shop." + key;
            String matName = config.getString(path + ".material");
            if (matName == null || matName.isEmpty()) {
                Bukkit.getLogger().warning("[Timerald:CustomItem] Missing material for: " + key);
                continue;
            }

            Material material;
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                Bukkit.getLogger().warning("[Timerald:CustomItem] Invalid material '" + matName + "' for item: " + key);
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
            items.put(key.toLowerCase(), item);
        }

        Bukkit.getLogger().info("[Timerald:CustomItem] Loaded " + items.size() + " custom items.");
    }


    public static ItemStack getItem(String key) {
        return items.get(key.toLowerCase());
    }


    public static boolean matches(ItemStack item, String idOrMaterialName) {
        if (idOrMaterialName == null || idOrMaterialName.equalsIgnoreCase("AIR")) {
            return item == null || item.getType() == Material.AIR;
        }

        String[] parts = idOrMaterialName.split(":");
        String base = parts[0].toLowerCase();

        // Check custom item
        if (items.containsKey(base)) {
            return matchesCustomItem(item, base);
        }

        // Vanilla material check
        try {
            Material mat = Material.valueOf(base.toUpperCase());
            if (item == null || item.getType() != mat) return false;

            // Potion variant check
            if (mat == Material.POTION && parts.length > 1) {
                if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
                try {
                    return meta.getBasePotionData().getType() == PotionType.valueOf(parts[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private static boolean matchesCustomItem(ItemStack item, String key) {
        if (item == null) return false;

        ItemStack expected = items.get(key.toLowerCase());
        if (expected == null || item.getType() != expected.getType()) return false;

        ItemMeta actualMeta = item.getItemMeta();
        ItemMeta expectedMeta = expected.getItemMeta();

        if (actualMeta == null || expectedMeta == null) return false;
        if (!Objects.equals(actualMeta.getDisplayName(), expectedMeta.getDisplayName())) return false;

        if (actualMeta.hasLore() && expectedMeta.hasLore()) {
            return Objects.equals(actualMeta.getLore(), expectedMeta.getLore());
        }

        return !actualMeta.hasLore() && !expectedMeta.hasLore();
    }




}
