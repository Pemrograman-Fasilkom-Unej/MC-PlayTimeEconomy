package com.darkun7.timerald.command;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.shop.ShopItem;
import com.darkun7.timerald.shop.OrderItem;
import com.darkun7.timerald.shop.ShopManager;
import com.darkun7.timerald.shop.PlayerShop;
import com.darkun7.timerald.util.Helpers;
import com.darkun7.timerald.gui.ShopGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.InventoryView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.HoverEvent.ShowItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;

public class ShopCommand implements CommandExecutor {

    private final Timerald plugin;
    private final ShopManager shopManager;
    private final ShopGUI shopGUI;

    public ShopCommand(Timerald plugin, ShopManager shopManager, ShopGUI shopGUI) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.shopGUI = shopGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use the shop.");
            return true;
        }
        
        UUID uuid = player.getUniqueId();

        if (args.length == 0 || args[0].equalsIgnoreCase("page")) {
            shopGUI.openPlayerShopSelectorGUI(player, 0);
            return true;
        }


        if (args[0].equalsIgnoreCase("stash")) {
            PlayerShop shop = shopManager.getOrCreateShop(uuid);
            shopGUI.openStashGUI(player, shop);
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

            PlayerShop shop = shopManager.getOrCreateShop(uuid);
            if (shop == null) {
                shop = shopManager.openShop(player);
            }

            ShopItem listing = new ShopItem(hand.clone(), (int) price, quantity);
            shop.addListing(listing);
            shopManager.savePlayerShop(uuid);

            // Message parts
            ItemMeta meta = hand.getItemMeta();
            String itemName = meta != null && meta.hasDisplayName()
                    ? meta.getDisplayName()
                    : Arrays.stream(hand.getType().name().toLowerCase().split("_"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(Collectors.joining(" "));

            Component itemNameComponent = Component.text(itemName, NamedTextColor.WHITE);

            Component clickableCommand = Component.text("/shop stash", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/shop stash"))
                    .hoverEvent(HoverEvent.showText(Component.text("Open stash", NamedTextColor.GRAY)));

            Component finalMessage = Component.text("Listing ")
                    .color(NamedTextColor.GREEN)
                    .append(itemNameComponent)
                    .append(Component.text(" created for " + price + " Timerald (@" + quantity + "). Stash the item via ", NamedTextColor.GREEN))
                    .append(clickableCommand)
                    .append(Component.text(".", NamedTextColor.GREEN));

            // Send message to player
            player.sendMessage(finalMessage);

            Helpers.broadcastListing(player, hand, price, quantity);
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

            // Remove the request
            ShopItem removedListing = shop.getListings().remove(index);
            shopManager.savePlayerShop(uuid);

            String itemName = Arrays.stream(removedListing.getItem().getType().name().toLowerCase().split("_"))
                                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                                .collect(Collectors.joining(" "));

            shopManager.savePlayerShop(uuid);

            player.sendMessage("§eCancelled listing #" + index + " for " + itemName + ".");
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
            
            shopGUI.openPlayerShopGUI(player, target.getUniqueId(), targetShop, 0);
            // player.sendMessage("§6Shop of " + target.getName() + ":");
            // List<ShopItem> listings = targetShop.getListings();
            // for (int i = 0; i < listings.size(); i++) {
            //     ShopItem item = listings.get(i);
            //     player.sendMessage(" §e[" + i + "] §f" + item.getItem().getType() + " x" + item.getQuantity() +
            //             " §7- §a" + item.getPrice() + " Timerald");
            // }
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

            // Check if buyer has space in inventory
            ItemStack toGive = item.getItem().clone();
            toGive.setAmount(item.getQuantity());

            // Try to remove from stash before taking money
            boolean success = sellerShop.removeItemFromStash(item.getItem(), item.getQuantity());
            if (!success) {
                player.sendMessage("§cThe seller no longer has enough of this item in their stash.");
                return true;
            } else {
                // Give item to buyer
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(toGive.clone());
                if (!remaining.isEmpty()) {
                    player.sendMessage("§cNot enough inventory space to complete the purchase.");
                    return true;
                }
                String itemName = Arrays.stream(toGive.getType().name().toLowerCase().split("_"))
                                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                                    .collect(Collectors.joining(" "));
                // Transfer Timerald
                plugin.getTimeraldManager().subtract(buyerId, item.getPrice());
                plugin.getTimeraldManager().add(sellerId, item.getPrice());

                player.sendMessage("§aPurchased " + itemName + " ×" + item.getQuantity() + " from " + seller.getName() + ".");

                Player onlineSeller = Bukkit.getPlayer(sellerId);
                if (onlineSeller != null && onlineSeller.isOnline()) {
                    onlineSeller.sendMessage("§a" + player.getName() + " bought " + itemName + " ×" + item.getQuantity() + " from your shop for " + item.getPrice() + " Timerald.");
                }
                return true;
            }     
        }

        // /shop order
        if (args[0].equalsIgnoreCase("order")) {
            if (args.length < 4) {
                player.sendMessage("§cUsage: /shop order <item:meta> <price> @<pcs> <limit:default(1)>");
                return true;
            }

            String itemArg = args[1]; // e.g. "potion:healing" or "diamond_sword"
            double price;
            int quantity;
            int limit = 1;

            // Parse price
            try {
                price = Double.parseDouble(args[2]);
                if (price <= 0) {
                    player.sendMessage("§cPrice must be greater than 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid price format.");
                return true;
            }

            // Parse quantity (must start with '@')
            if (!args[3].startsWith("@")) {
                player.sendMessage("§cQuantity must start with '@'.");
                return true;
            }
            try {
                quantity = Integer.parseInt(args[3].substring(1));
                if (quantity <= 0) {
                    player.sendMessage("§cQuantity must be greater than 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid quantity format.");
                return true;
            }

            // Parse optional limit
            if (args.length >= 5) {
                try {
                    limit = Integer.parseInt(args[4]);
                    if (limit <= 0) limit = 1;
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid limit format. Using default 1.");
                    limit = 1;
                }
            }

            // Now create the ItemStack from itemArg (handle meta)
            // This requires a helper method. Here's a simple example:
            ItemStack item;
            try {
                item = Helpers.parseItemFromString(itemArg);
                if (item == null || item.getType() == Material.AIR) {
                    player.sendMessage("§cInvalid item specified.");
                    return true;
                }
            } catch (Exception e) {
                player.sendMessage("§cFailed to parse item: " + e.getMessage());
                return true;
            }

            // Set the amount on the item stack to quantity requested
            item.setAmount(limit);

            // Get or create shop
            PlayerShop shop = shopManager.getOrCreateShop(uuid);
            if (shop == null) {
                shop = shopManager.openShop(player);
            }

            OrderItem request = new OrderItem(item, (int) price, quantity, limit);
            shop.addRequest(request);
            shopManager.savePlayerShop(uuid);

            String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().getDisplayName()
                    : Arrays.stream(item.getType().name().toLowerCase().split("_"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(Collectors.joining(" "));

            Component itemNameComponent = Component.text(itemName, NamedTextColor.WHITE);

            Component finalMessage = Component.text("Buy request ")
                    .color(NamedTextColor.GREEN)
                    .append(itemNameComponent)
                    .append(Component.text(" created for " + price + " Timerald (@" + quantity + ")", NamedTextColor.GREEN))
                    .append(Component.text(".", NamedTextColor.GREEN));

            player.sendMessage(finalMessage);
            Helpers.broadcastRequest(player, itemName, price, quantity, limit);
            return true;
        }

        // /shop fulfill <player> <index>
        if (args[0].equalsIgnoreCase("fulfill")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /shop fulfill <player> <index>");
                return true;
            }

            OfflinePlayer requester = Bukkit.getOfflinePlayer(args[1]);
            int index;
            try {
                index = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid index.");
                return true;
            }

            PlayerShop requesterShop = shopManager.getShop(requester.getUniqueId());
            if (requesterShop == null || index < 0 || index >= requesterShop.getRequests().size()) {
                player.sendMessage("§cInvalid request.");
                return true;
            }

            OrderItem request = requesterShop.getRequests().get(index);
            UUID fulfillerId = player.getUniqueId();
            UUID requesterId = requester.getUniqueId();

            // Check if fulfiller has enough items in inventory
            int neededQty = request.getQuantity();
            int totalFound = Helpers.getTotalMatchingItems(
                Arrays.asList(player.getInventory().getContents()), request.getItem()
            );

            if (totalFound < neededQty) {
                player.sendMessage("§cYou don’t have enough items to fulfill this request.");
                return true;
            }

            // Check if requester has enough Timerald to pay
            if (plugin.getTimeraldManager().get(requesterId) < request.getPrice()) {
                player.sendMessage("§cThe requester does not have enough Timerald to pay for this request.");
                return true;
            }

            // Prepare the item to deliver
            ItemStack toDeliver = request.getItem().clone();
            toDeliver.setAmount(request.getQuantity());

            // Check if requester's stash can fit the item(s)
            if (!requesterShop.canAddToStash(toDeliver)) {
                player.sendMessage("§cThe requester's stash is full or does not have enough space for the items.");
                return true;
            }

            // Remove items from fulfiller's inventory
            int remaining = neededQty;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.isSimilar(request.getItem())) {
                    int amt = invItem.getAmount();
                    if (amt > remaining) {
                        invItem.setAmount(amt - remaining);
                        remaining = 0;
                        break;
                    } else {
                        player.getInventory().removeItem(invItem);
                        remaining -= amt;
                        if (remaining <= 0) break;
                    }
                }
            }

            // Add items to requester's stash
            requesterShop.addToStash(toDeliver);
            shopManager.savePlayerShop(requesterId);

            // Transfer Timerald from requester to fulfiller
            plugin.getTimeraldManager().subtract(requesterId, request.getPrice());
            plugin.getTimeraldManager().add(fulfillerId, request.getPrice());

            String itemName = Arrays.stream(toDeliver.getType().name().toLowerCase().split("_"))
                                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                                .collect(Collectors.joining(" "));

            player.sendMessage("§aDelivered §f" + itemName + "§a ×" + neededQty + " to §f" + requester.getName() +
                            "§a for §b" + request.getPrice() + " Timerald§a.");

            Player onlineRequester = Bukkit.getPlayer(requesterId);
            if (onlineRequester != null && onlineRequester.isOnline()) {
                onlineRequester.sendMessage("§f" + player.getName() + "§a fulfilled your request for §f" + 
                                            itemName + "§a ×" + neededQty + " for §b" + request.getPrice() + " Timerald§a.");
            }

            // Reduce request limit and remove if complete
            request.setLimit(request.getLimit() - 1); // assuming limit tracks remaining fulfillments
            if (request.getLimit() <= 0) {
                requesterShop.getRequests().remove(index);
                if (onlineRequester != null && onlineRequester.isOnline()) {
                    onlineRequester.sendMessage("§eYour request for §f" + itemName + "§e has been completed and order removed.");
                }
            }

            shopManager.savePlayerShop(requesterId);
            return true;
        }

        // /shop cancel-order <index>
        if (args[0].equalsIgnoreCase("cancel-order")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /shop cancel-order <index>");
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
            if (shop == null || index < 0 || index >= shop.getRequests().size()) {
                player.sendMessage("§cInvalid request index.");
                return true;
            }

            // Remove the request
            OrderItem removedRequest = shop.getRequests().remove(index);
            shopManager.savePlayerShop(uuid);
            
            String itemName = Arrays.stream(removedRequest.getItem().getType().name().toLowerCase().split("_"))
                                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                                .collect(Collectors.joining(" "));

            player.sendMessage("§eCancelled request #" + index + " for " + itemName + " ×" + removedRequest.getQuantity() + ".");
            return true;
        }

        if (args[0].equalsIgnoreCase("requests")) {
            PlayerShop shop = shopManager.getOrCreateShop(uuid);
            shopGUI.openRequestsUI(player, 0);
            return true;
        }

        player.sendMessage("§cUnknown subcommand.");
        return true;
    }

}
