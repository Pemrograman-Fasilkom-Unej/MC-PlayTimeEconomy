package com.darkun7.timerald.command;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.shop.ShopItem;
import com.darkun7.timerald.shop.ShopManager;
import com.darkun7.timerald.shop.PlayerShop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ShopCommand implements CommandExecutor, Listener {

    private final Timerald plugin;
    private final ShopManager shopManager;

    private final int SHOP_OPEN_COST = 10; // Timerald required to open shop

    public ShopCommand(Timerald plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use the shop.");
            return true;
        }
        
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            // /shop: list shops
            player.sendMessage("§6Open Shops:");
            for (PlayerShop ps : shopManager.getOpenShops()) {
                if (!ps.getListings().isEmpty()) {
                    player.sendMessage(" - " + Bukkit.getOfflinePlayer(ps.getOwner()).getName());
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stash")) {
            PlayerShop shop = shopManager.getOrCreateShop(uuid);
            openStashGUI(player, shop);
            return true;
        }

        // /shop sell
        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /shop sell <price> @<pcs>");
                return true;
            }

            double price;
            int quantity;
            try {
                price = Double.parseDouble(args[1]);
                if (!args[2].startsWith("@")) throw new NumberFormatException();
                quantity = Integer.parseInt(args[2].substring(1));

                if (price <= 0 || quantity <= 0) {
                    player.sendMessage("§cInvalid value Price or quantity");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid format. Use: /shop sell <price> @<pcs>");
                return true;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage("§cYou must hold an item to sell.");
                return true;
            }

            PlayerShop shop = shopManager.getShop(uuid);
            if (shop == null) {
                if (plugin.getTimeraldManager().get(uuid) < SHOP_OPEN_COST) {
                    player.sendMessage("§cNot enough Timerald to open a shop. Need " + SHOP_OPEN_COST + ".");
                    return true;
                }
                plugin.getTimeraldManager().subtract(uuid, SHOP_OPEN_COST);
                shop = shopManager.openShop(player);
                player.sendMessage("§aShop opened for " + SHOP_OPEN_COST + " Timerald!");
            }

            ShopItem listing = new ShopItem(hand.clone(), (int) price, quantity);
            shop.addListing(listing);
            shopManager.savePlayerShop(uuid);

            player.sendMessage("§aListing created for " + price + " Timerald (@"+ quantity +"). You must stash the items separately.");
            return true;
        }

        // /shop cancel
        if (args[0].equalsIgnoreCase("cancel")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /shop cancel <index>");
                return true;
            }

            int index;
            try {
                index = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid index.");
                return true;
            }

            PlayerShop shop = shopManager.getShop(uuid);
            if (shop == null || index < 0 || index >= shop.getListings().size()) {
                player.sendMessage("§cInvalid listing index.");
                return true;
            }

            shop.getListings().remove(index);
            shopManager.savePlayerShop(uuid);

            player.sendMessage("§eCancelled listing #" + index + ".");
            return true;
        }

        // /shop visit
        if (args[0].equalsIgnoreCase("visit")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /shop visit <player>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            PlayerShop targetShop = shopManager.getShop(target.getUniqueId());

            if (targetShop == null || targetShop.getListings().isEmpty()) {
                player.sendMessage("§cThat player has no active shop or no listings.");
                return true;
            }

            player.sendMessage("§6Shop of " + target.getName() + ":");
            List<ShopItem> listings = targetShop.getListings();
            for (int i = 0; i < listings.size(); i++) {
                ShopItem item = listings.get(i);
                player.sendMessage(" §e[" + i + "] §f" + item.getItem().getType() + " x" + item.getQuantity() +
                        " §7- §a" + item.getPrice() + " Timerald");
            }
            return true;
        }

        // /shop buy
        if (args[0].equalsIgnoreCase("buy")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /shop buy <player> <index>");
                return true;
            }

            OfflinePlayer seller = Bukkit.getOfflinePlayer(args[1]);
            int index;
            try {
                index = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid index.");
                return true;
            }

            PlayerShop sellerShop = shopManager.getShop(seller.getUniqueId());
            if (sellerShop == null || index < 0 || index >= sellerShop.getListings().size()) {
                player.sendMessage("§cInvalid listing.");
                return true;
            }

            ShopItem item = sellerShop.getListings().get(index);
            UUID buyerId = player.getUniqueId();
            UUID sellerId = seller.getUniqueId();

            // Check if buyer has enough Timerald
            if (plugin.getTimeraldManager().get(buyerId) < item.getPrice()) {
                player.sendMessage("§cNot enough Timerald.");
                return true;
            }

            // Try to remove from stash before taking money
            boolean success = sellerShop.removeItemFromStash(item.getItem(), item.getQuantity());
            if (!success) {
                player.sendMessage("§cThe seller no longer has enough of this item in their stash.");
                return true;
            }

            // Transfer Timerald
            plugin.getTimeraldManager().subtract(buyerId, item.getPrice());
            plugin.getTimeraldManager().add(sellerId, item.getPrice());

            // Give item to buyer
            ItemStack toGive = item.getItem().clone();
            toGive.setAmount(item.getQuantity());
            player.getInventory().addItem(toGive);

            player.sendMessage("§aPurchased " + toGive.getType() + " x" + item.getQuantity() + " from " + seller.getName() + ".");

            return true;
        }


        player.sendMessage("§cUnknown subcommand.");
        return true;
    }

    public void openStashGUI(Player player, PlayerShop shop) {
        Inventory inv = Bukkit.createInventory(player, 54, "Your Shop Stash");
        for (ItemStack item : shop.getStash()) {
            inv.addItem(item.clone());
        }
        player.openInventory(inv);
    }


    @EventHandler
    public void onStashClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("Your Shop Stash")) return;

        UUID uuid = player.getUniqueId();
        PlayerShop shop = shopManager.getOrCreateShop(uuid);

        shop.getStash().clear();
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                shop.addToStash(item);
            }
        }

        shopManager.savePlayerShop(uuid);
        player.sendMessage("§aYour stash has been saved.");
    }

}
