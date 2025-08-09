package com.darkun7.timerald.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.FireworkEffect;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import java.util.List;


public class Helpers {
    public static ItemStack parseItemFromString(String input) {
        // Split input like "potion:healing", "enchanted_book:sharpness_5", "leather_boots:trim_gold"
        String[] parts = input.split(":", 2);
        Material material = Material.matchMaterial(parts[0].toUpperCase());
        if (material == null) return null;

        ItemStack item = new ItemStack(material);

        if (parts.length == 2) {
            String metaArg = parts[1].toLowerCase();
            ItemMeta meta = item.getItemMeta();

            switch (material) {
                case POTION:
                case SPLASH_POTION:
                case LINGERING_POTION:
                    if (meta instanceof PotionMeta potionMeta) {
                        try {
                            PotionType potionType = PotionType.valueOf(metaArg.toUpperCase());
                            potionMeta.setBasePotionData(new PotionData(potionType));
                            item.setItemMeta(potionMeta);
                        } catch (IllegalArgumentException e) {
                            // Unknown potion type, try effect type fallback if needed
                            // Could add custom effect logic here
                        }
                    }
                    break;

                case ENCHANTED_BOOK:
                    if (meta instanceof EnchantmentStorageMeta enchMeta) {
                        // Expect format like "sharpness_5"
                        String[] enchParts = metaArg.split("_");
                        if (enchParts.length == 2) {
                            Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchParts[0]));
                            if (enchant == null) enchant = Enchantment.getByName(enchParts[0].toUpperCase());
                            if (enchant != null) {
                                try {
                                    int level = Integer.parseInt(enchParts[1]);
                                    enchMeta.addStoredEnchant(enchant, level, true);
                                    item.setItemMeta(enchMeta);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    break;

                case LEATHER_BOOTS:
                case LEATHER_LEGGINGS:
                case LEATHER_CHESTPLATE:
                case LEATHER_HELMET:
                    // Check if metaArg starts with "trim_"
                    if (meta instanceof LeatherArmorMeta leatherMeta && metaArg.startsWith("trim_")) {
                        // Simple example: trim_gold, trim_red, etc.
                        String trimColorName = metaArg.substring("trim_".length());
                        // Bukkit does not have direct trim API, so this might be custom or mock:
                        // For demo, just set leather color from name:
                        try {
                            Color color = parseColorFromName(trimColorName);
                            leatherMeta.setColor(color);
                            item.setItemMeta(leatherMeta);
                        } catch (Exception ignored) {}
                    }
                    break;

                case FIREWORK_ROCKET:
                    if (meta instanceof FireworkMeta fireworkMeta) {
                        // For demo, create a simple firework with one color burst
                        FireworkEffect effect = FireworkEffect.builder()
                                .withColor(parseColorFromName(metaArg))
                                .with(FireworkEffect.Type.BALL)
                                .build();
                        fireworkMeta.addEffect(effect);
                        item.setItemMeta(fireworkMeta);
                    }
                    break;

                case MUSIC_DISC_13:
                case MUSIC_DISC_CAT:
                case MUSIC_DISC_BLOCKS:
                case MUSIC_DISC_CHIRP:
                case MUSIC_DISC_FAR:
                case MUSIC_DISC_MALL:
                case MUSIC_DISC_MELLOHI:
                case MUSIC_DISC_STAL:
                case MUSIC_DISC_STRAD:
                case MUSIC_DISC_WARD:
                case MUSIC_DISC_11:
                case MUSIC_DISC_WAIT:
                    // Support specifying disc by name like "cat", "chirp"
                    Material discMat = getMusicDiscByName(metaArg);
                    if (discMat != null) {
                        item.setType(discMat);
                    }
                    break;

                case TIPPED_ARROW:
                    if (meta instanceof PotionMeta tippedMeta) {
                        try {
                            PotionType potionType = PotionType.valueOf(metaArg.toUpperCase());
                            tippedMeta.setBasePotionData(new PotionData(potionType));
                            item.setItemMeta(tippedMeta);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    break;

                default:
                    // For any other item, maybe support display name override? Or do nothing
                    break;
            }
        }

        return item;
    }

    // Helper to parse Bukkit Color from simple names (red, blue, gold, etc.)
    public static Color parseColorFromName(String name) {
        return switch (name.toLowerCase()) {
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "green" -> Color.GREEN;
            case "yellow" -> Color.YELLOW;
            case "gold" -> Color.fromRGB(255, 215, 0);
            case "white" -> Color.WHITE;
            case "black" -> Color.BLACK;
            case "purple" -> Color.fromRGB(128, 0, 128);
            case "orange" -> Color.fromRGB(255, 165, 0);
            case "pink" -> Color.fromRGB(255, 192, 203);
            default -> Color.WHITE; // fallback
        };
    }

    // Helper to get music disc Material by short name
    public static Material getMusicDiscByName(String name) {
        return switch (name.toLowerCase()) {
            case "13" -> Material.MUSIC_DISC_13;
            case "cat" -> Material.MUSIC_DISC_CAT;
            case "blocks" -> Material.MUSIC_DISC_BLOCKS;
            case "chirp" -> Material.MUSIC_DISC_CHIRP;
            case "far" -> Material.MUSIC_DISC_FAR;
            case "mall" -> Material.MUSIC_DISC_MALL;
            case "mellohi" -> Material.MUSIC_DISC_MELLOHI;
            case "stal" -> Material.MUSIC_DISC_STAL;
            case "strad" -> Material.MUSIC_DISC_STRAD;
            case "ward" -> Material.MUSIC_DISC_WARD;
            case "11" -> Material.MUSIC_DISC_11;
            case "wait" -> Material.MUSIC_DISC_WAIT;
            default -> null;
        };
    }

    public static int getTotalMatchingItems(List<ItemStack> stash, ItemStack target) {
        int total = 0;
        for (ItemStack item : stash) {
            if (item != null && item.isSimilar(target)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static void broadcastListing(Player sender, ItemStack item, double price, int quantity) {
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
        ? item.getItemMeta().getDisplayName()
        : formatMaterialName(item.getType());

        Component message = Component.text("§b⬥WTS⬥ §f" + sender.getName() + "§7 is selling §f" + itemName +
                " §7for §b" + price + " Timerald §7(x" + quantity + ") ")
            .append(Component.text("§e[Open Shop]")
                .clickEvent(ClickEvent.runCommand("/shop visit " + sender.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("§7Visit shop")))
                );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(sender)) {
                player.sendMessage(message);
            }
        }
    }

    public static void broadcastRequest(Player sender, String itemName, double price, int quantity, int limit) {
        Component message = Component.text("§3⬥WTB⬥ §f" + sender.getName() + "§7 is requesting §f" + itemName +
                " §7for §b" + price + " Timerald §7(x" + quantity + ") with limit " + limit + ". ")
            .append(Component.text("§e[Open Shop]")
                .clickEvent(ClickEvent.runCommand("/shop requests"))
                .hoverEvent(HoverEvent.showText(Component.text("§7Visit shop")))
                );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(sender)) {
                player.sendMessage(message);
            }
        }
    }

    public static String formatMaterialName(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }
}