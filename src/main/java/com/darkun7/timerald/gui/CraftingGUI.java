package com.darkun7.timerald.gui;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.CustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class CraftingGUI {

    public static final String TITLE = "§8Custom Crafting";
    public static final List<Integer> CRAFT_SLOTS = Arrays.asList(
        10, 11, 12,
        19, 20, 21,
        28, 29, 30
    );
    public static final int CRAFT_BUTTON_SLOT = 23;
    public static final int GUIDE_BUTTON_SLOT = 36;
    public static final int RESULT_SLOT = 25;

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        // Glass pane filler
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 45; i++) {
            inv.setItem(i, glass);
        }

        // Clear crafting grid
        for (int slot : CRAFT_SLOTS) {
            inv.setItem(slot, null);
        }

        // Craft button
        ItemStack executeButton = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta execMeta = executeButton.getItemMeta();
        execMeta.setDisplayName("§a§lCraft");
        executeButton.setItemMeta(execMeta);
        inv.setItem(CRAFT_BUTTON_SLOT, executeButton);

        // Empty result slot
        inv.setItem(RESULT_SLOT, null);

        // Recipe Book
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.setDisplayName("§e§lRecipe Guide");
        bookMeta.setLore(Arrays.asList(
            "§7Click to view all recipes",
            "§7Select a recipe to preview and craft"
        ));
        book.setItemMeta(bookMeta);
        inv.setItem(36, book);

        player.openInventory(inv);
    }

    public static void handleClick(InventoryClickEvent event, Timerald plugin) {
        if (!event.getView().getTitle().equals(TITLE)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // Allow player inventory interaction
        if (slot >= event.getInventory().getSize()) {
            // Handle shift-click from player inventory into crafting grid
            if (event.isShiftClick()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> 
                    updatePreview(event.getInventory(), plugin), 1L);
            }
            return;
        }

        // Allow crafting grid interaction
        if (CRAFT_SLOTS.contains(slot)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> 
                updatePreview(event.getInventory(), plugin), 1L);
            return;
        }

        // Cancel other slots
        event.setCancelled(true);

        // Handle craft button
        if (slot == CRAFT_BUTTON_SLOT) {
            ItemStack preview = event.getInventory().getItem(RESULT_SLOT);
            if (preview == null || preview.getType() == Material.AIR) {
                player.sendMessage("§cInvalid recipe!");
                return;
            }

            for (String recipeKey : plugin.getConfig().getConfigurationSection("crafting").getKeys(false)) {
                List<String> shape = plugin.getConfig().getStringList("crafting." + recipeKey + ".shape");
                if (matchesRecipe(event.getInventory(), shape)) {

                    // Consume ingredients
                    consumeIngredients(event.getInventory(), shape);

                    // Give result to player
                    ItemStack result = preview.clone(); // ✅ clone to avoid GUI slot reference
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result);
                    if (!leftover.isEmpty()) {
                        // If inventory full → drop on ground
                        for (ItemStack item : leftover.values()) {
                            player.getWorld().dropItem(player.getLocation(), item);
                        }
                    }
                    
                    if (recipeKey != null) {
                        String resultName = Arrays.stream(recipeKey.split("-"))
                                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                                .collect(Collectors.joining(" "));

                        player.sendMessage("§aCrafting successful: §f" + resultName);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    } else {
                        player.sendMessage("§cCrafting failed: Invalid recipe key!");
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

                    // Clear result slot and refresh preview
                    event.getInventory().setItem(RESULT_SLOT, null);
                    updatePreview(event.getInventory(), plugin);
                    break;
                }
            }
        }
        if (slot == GUIDE_BUTTON_SLOT) { // Recipe book slot
            event.setCancelled(true);
            openRecipeList(player, plugin);
            return;
        }


    }

    public static void updatePreview(Inventory inv, Timerald plugin) {
        for (String recipeKey : plugin.getConfig().getConfigurationSection("crafting").getKeys(false)) {
            List<String> shape = plugin.getConfig().getStringList("crafting." + recipeKey + ".shape");

            if (matchesRecipe(inv, shape)) {
                String resultItem = plugin.getConfig().getString("crafting." + recipeKey + ".result.item");
                String variation = plugin.getConfig().getString("crafting." + recipeKey + ".result.variation", "custom");

                ItemStack result;
                if ("custom".equalsIgnoreCase(variation)) {
                    result = CustomItemManager.getItem(resultItem); // ✅ now safe with lowercase
                } else {
                    try {
                        result = new ItemStack(Material.valueOf(resultItem.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        result = null;
                    }
                }

                inv.setItem(RESULT_SLOT, result != null ? result.clone() : null);
                return;
            }
        }
        inv.setItem(RESULT_SLOT, null); // no match
    }


    private static boolean matchesRecipe(Inventory inv, List<String> shape) {
        if (shape.size() != 3) return false;

        for (int i = 0; i < CRAFT_SLOTS.size(); i++) {
            String[] row = shape.get(i / 3).trim().split(" ");
            if (row.length != 3) return false;

            String expected = row[i % 3].trim();
            ItemStack actual = inv.getItem(CRAFT_SLOTS.get(i));

            boolean match = CustomItemManager.matches(actual, expected);
            if (!match) {
                // Bukkit.getLogger().info("[CraftDebug] Slot " + CRAFT_SLOTS.get(i) +
                //     " expected: " + expected +
                //     " but got: " + describeItem(actual));
                return false;
            }
        }
        return true;
    }

    private static String describeItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "AIR";
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : "(no name)";
        return item.getType() + " " + name;
    }

    private static void consumeIngredients(Inventory inv, List<String> shape) {
        for (int i = 0; i < CRAFT_SLOTS.size(); i++) {
            String expected = shape.get(i / 3).split(" ")[i % 3];
            if (!expected.equalsIgnoreCase("AIR")) {
                ItemStack stack = inv.getItem(CRAFT_SLOTS.get(i));
                if (stack != null && stack.getType() != Material.AIR) {
                    stack.setAmount(stack.getAmount() - 1);
                    if (stack.getAmount() <= 0) {
                        inv.setItem(CRAFT_SLOTS.get(i), new ItemStack(Material.AIR));
                    } else {
                        inv.setItem(CRAFT_SLOTS.get(i), stack);
                    }
                }
            }
        }
    }
    // RECIPE SECTION
    public static void openRecipeList(Player player, Timerald plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Recipe List");

        int index = 0;
        for (String recipeKey : plugin.getConfig().getConfigurationSection("crafting").getKeys(false)) {
            String resultItem = plugin.getConfig().getString("crafting." + recipeKey + ".result.item");
            String variation = plugin.getConfig().getString("crafting." + recipeKey + ".result.variation", "custom");

            ItemStack icon;
            if ("custom".equalsIgnoreCase(variation)) {
                icon = CustomItemManager.getItem(resultItem);
            } else {
                try {
                    icon = new ItemStack(Material.valueOf(resultItem.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    continue; // skip invalid recipes
                }
            }

            if (icon != null) {
                ItemMeta meta = icon.getItemMeta();
                icon.setItemMeta(meta);
                inv.setItem(index++, icon);
            }
        }

        player.openInventory(inv);
    }

    public static void onRecipePreviewClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("§8Recipe: ")) return;

        event.setCancelled(true); // ❌ Prevent item movement

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot == CRAFT_BUTTON_SLOT) {
            // Only the craft button is interactive
            String recipeKey = event.getView().getTitle().substring("§8Recipe: ".length());
            // handleRecipeCraft(player, recipeKey);
            open(player);
        }
    }


    public static void onRecipeListClick(InventoryClickEvent event, Timerald plugin) {
        if (!event.getView().getTitle().equals("§8Recipe List")) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // If clicked outside the recipe area → ignore
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        List<String> recipes = new ArrayList<>(plugin.getConfig().getConfigurationSection("crafting").getKeys(false));
        if (slot >= recipes.size()) return; // ❌ Prevent index out of bounds

        String clickedKey = recipes.get(slot);
        openRecipePreview(player, plugin, clickedKey);
    }

    public static void openRecipePreview(Player player, Timerald plugin, String recipeKey) {
        String RecipeName = Arrays.stream(recipeKey.split("-"))
            .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
        Inventory inv = Bukkit.createInventory(null, 45, "§8Recipe: " + RecipeName);

        // Glass filler
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.setDisplayName(" ");
        glass.setItemMeta(gMeta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);

        // Place ingredients in 3x3
        List<String> shape = plugin.getConfig().getStringList("crafting." + recipeKey + ".shape");
        for (int i = 0; i < CRAFT_SLOTS.size(); i++) {
            String[] row = shape.get(i / 3).trim().split(" ");
            String ingredient = row[i % 3].trim();

            if (!ingredient.equalsIgnoreCase("AIR")) {
                // Check for custom item first
                ItemStack customItem = CustomItemManager.getItem(ingredient);
                if (customItem != null) {
                    inv.setItem(CRAFT_SLOTS.get(i), customItem.clone());
                } else {
                    String[] parts = ingredient.split(":");
                    String matName = parts[0].toUpperCase();

                    try {
                        Material mat = Material.valueOf(matName);
                        ItemStack stack = new ItemStack(mat);

                        // Handle potion variants for legacy versions
                        if (mat == Material.POTION && parts.length > 1) {
                            String variant = parts[1].toUpperCase();
                            ItemMeta meta = stack.getItemMeta();
                            if (meta instanceof PotionMeta) {
                                PotionMeta potionMeta = (PotionMeta) meta;
                                try {
                                    // 1.9+ servers
                                    potionMeta.setBasePotionData(new org.bukkit.potion.PotionData(
                                        org.bukkit.potion.PotionType.valueOf(variant)
                                    ));
                                } catch (NoClassDefFoundError | IllegalArgumentException e) {
                                    // 1.8 fallback: just rename it
                                    potionMeta.setDisplayName("§b" + variant + " Potion");
                                }
                                stack.setItemMeta(potionMeta);
                            }
                        }
                        inv.setItem(CRAFT_SLOTS.get(i), stack);
                    } catch (Exception ignored) {
                        inv.setItem(CRAFT_SLOTS.get(i), null);
                    }
                }
            } else {
                inv.setItem(CRAFT_SLOTS.get(i), null);
            }
        }

        // Craft button
        ItemStack craftButton = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta btnMeta = craftButton.getItemMeta();
        btnMeta.setDisplayName("§a§lCraft This");
        craftButton.setItemMeta(btnMeta);
        inv.setItem(CRAFT_BUTTON_SLOT, craftButton);

        // Result
        String resultItem = plugin.getConfig().getString("crafting." + recipeKey + ".result.item");
        String variation = plugin.getConfig().getString("crafting." + recipeKey + ".result.variation", "custom");
        ItemStack result = "custom".equalsIgnoreCase(variation) ?
                CustomItemManager.getItem(resultItem) :
                new ItemStack(Material.valueOf(resultItem.toUpperCase()));
        inv.setItem(RESULT_SLOT, result);

        player.openInventory(inv);
    }






}

