package com.darkun7.timerald.listener;

import com.darkun7.timerald.gui.CraftingGUI;
import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.CustomItemManager;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;

import com.darkun7.timerald.gui.CraftingGUI;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class CraftingInteractListener implements Listener {

    private final Timerald plugin;

    public CraftingInteractListener(Timerald plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("CraftingInteractListener initialized");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        CraftingGUI.handleClick(event, plugin);
        CraftingGUI.onRecipeListClick(event, plugin);
        CraftingGUI.onRecipePreviewClick(event);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked.getType() != Material.CRAFTING_TABLE) return;
        if (!event.getPlayer().isSneaking()) return;
        CraftingGUI.open(event.getPlayer());
        // Prevent normal crafting table GUI
        event.setCancelled(true);

        // Check block below for DEBUG
        Block below = clicked.getRelative(BlockFace.DOWN);
        if (below.getType() != Material.OBSIDIAN) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() != org.bukkit.Material.AIR) {
            int newAmount = item.getAmount() * 4;
            int maxStack = item.getMaxStackSize();
            if (newAmount > maxStack) {
                newAmount = maxStack;
                item.setAmount(newAmount);
                player.getInventory().setItemInMainHand(item);
            } else {
                player.getInventory().addItem(item.clone());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(CraftingGUI.TITLE)) return;

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        // Collect items first
        List<ItemStack> toReturn = new ArrayList<>();

        for (int slot : CraftingGUI.CRAFT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                toReturn.add(item.clone());
                inv.setItem(slot, new ItemStack(Material.AIR));
            }
        }

        // ItemStack result = inv.getItem(CraftingGUI.RESULT_SLOT);
        // if (result != null && result.getType() != Material.AIR) {
        //     toReturn.add(result.clone());
        //     inv.setItem(CraftingGUI.RESULT_SLOT, new ItemStack(Material.AIR));
        // }

        // Return items AFTER clearing inventory
        for (ItemStack item : toReturn) {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(leftover ->
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover)
                );
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(CraftingGUI.TITLE)) return;

        // If drag affects any crafting slots → update preview next tick
        for (int slot : event.getRawSlots()) {
            if (CraftingGUI.CRAFT_SLOTS.contains(slot)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> 
                    CraftingGUI.updatePreview(event.getInventory(), plugin), 1L);
                break;
            }
        }
    }




}