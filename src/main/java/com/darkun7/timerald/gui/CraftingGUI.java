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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;


public class CraftingGUI {

    public static final String TITLE = "§8Custom Crafting";
    public static final List<Integer> CRAFT_SLOTS = Arrays.asList(
        10, 11, 12, 
        19, 20, 21, 
        28, 29, 30
    );
    public static final int CRAFT_BUTTON_SLOT = 23;
    public static final int RESULT_SLOT = 25;

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "§8Custom Crafting");

        // Glass pane filler item
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        // Fill all with glass first
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, glass);
        }

        // Clear input slots (crafting grid: 3x3 center)
        int[] inputSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int slot : inputSlots) {
            inv.setItem(slot, null); // Leave empty for player to input
        }

        // Set "Craft" button (slot 23)
        ItemStack executeButton = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta execMeta = executeButton.getItemMeta();
        execMeta.setDisplayName("§a§lCraft");
        executeButton.setItemMeta(execMeta);
        inv.setItem(23, executeButton);

        // Set result slot (slot 25) as empty
        inv.setItem(25, null); // Player sees result after clicking "Craft"

        player.openInventory(inv);
    }

    public static void handleClick(InventoryClickEvent event, Timerald plugin) {
        if (!event.getView().getTitle().equals(TITLE)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // Allow player inventory interaction
        if (slot >= event.getInventory().getSize()) return;

        // Allow crafting grid interaction
        if (CRAFT_SLOTS.contains(slot)) {
            return;
        }

        // Cancel other slots (e.g. glass panes, result slot)
        event.setCancelled(true);

        // Handle crafting logic
        if (slot == CRAFT_BUTTON_SLOT) {
            List<String> shape = plugin.getConfig().getStringList("crafting.potion-of-flight.shape");

            boolean match = true;
            for (int i = 0; i < CRAFT_SLOTS.size(); i++) {
                String[] row = shape.get(i / 3).split(" ");
                if (row.length != 3) {
                    // plugin.getLogger().warning("Invalid shape format in config at row: " + (i / 3));
                    player.sendMessage("§cCraft config error! Check shape row: " + (i / 3));
                    return;
                }

                String expected = row[i % 3];
                ItemStack actual = event.getInventory().getItem(CRAFT_SLOTS.get(i));

                // Debug: show what is expected and what was placed
                // plugin.getLogger().info("Checking slot " + CRAFT_SLOTS.get(i) + " → Expected: " + expected + ", Actual: " + describeItem(actual));

                if (!CustomItemManager.matches(actual, expected)) {
                    match = false;
                    // plugin.getLogger().info("Mismatch found at slot " + CRAFT_SLOTS.get(i));
                    break;
                }
            }

            if (match) {
                // Consume one of each required ingredient
                for (int i = 0; i < CRAFT_SLOTS.size(); i++) {
                    String expected = shape.get(i / 3).split(" ")[i % 3];
                    if (!expected.equalsIgnoreCase("AIR")) {
                        ItemStack stack = event.getInventory().getItem(CRAFT_SLOTS.get(i));
                        if (stack != null && stack.getType() != Material.AIR) {
                            stack.setAmount(stack.getAmount() - 1);
                            if (stack.getAmount() <= 0) {
                                event.getInventory().setItem(CRAFT_SLOTS.get(i), new ItemStack(Material.AIR));
                            } else {
                                event.getInventory().setItem(CRAFT_SLOTS.get(i), stack);
                            }
                        }
                    }
                }

                // Place result
                ItemStack result = CustomItemManager.getItem("wormhole-potion");
                if (result != null) {
                    player.sendMessage("§aCrafting successful!");
                    event.getInventory().setItem(RESULT_SLOT, result);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                } else {
                    player.sendMessage("§cError: Result item not found!");
                }
            } else {
                player.sendMessage("§cInvalid recipe!");
            }

        }

    }

    private static String describeItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "AIR";
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName() : "no name";
        return item.getType() + " (" + name + ")";
    }


    


}
