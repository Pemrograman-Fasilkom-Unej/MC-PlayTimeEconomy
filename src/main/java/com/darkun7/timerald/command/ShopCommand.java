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

public class ShopCommand implements CommandExecutor, Listener {

    private final Timerald plugin;
    private final ShopManager shopManager;

    private static final String SHOP_GUI_PREFIX = "§2Shops §7(";
    private static final String PLAYER_SHOP_GUI_PREFIX = "§2Shop: §e";
    private final Set<UUID> recentlyClicked = new HashSet<>();

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

        if (args.length == 0 || args[0].equalsIgnoreCase("page")) {
            openPlayerShopSelectorGUI(player, 0);
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

            // Message parts
            ItemMeta meta = hand.getItemMeta();
            String rawName = meta != null && meta.hasDisplayName()
                    ? meta.getDisplayName()
                    : hand.getType().name().toLowerCase().replace("_", " ");

            Component itemNameComponent = Component.text(rawName, NamedTextColor.WHITE);

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

            // Optionally broadcast item to others
            broadcastListing(player, hand, price, quantity);
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
            
            openPlayerShopGUI(player, target.getUniqueId(), targetShop, 0);
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

    // UI: SHOP
    private void openPlayerShopSelectorGUI(Player player, int page) {
        List<PlayerShop> openShops = shopManager.getOpenShops().stream()
            .filter(shop -> !shop.getListings().isEmpty())
            .toList();

        int totalShops = openShops.size();
        int perPage = 45;
        int maxPage = (int) Math.ceil((double) totalShops / perPage);
        if (page >= maxPage) page = maxPage - 1;
        if (page < 0) page = 0;

        Inventory gui = Bukkit.createInventory(null, 54, SHOP_GUI_PREFIX + "Page " + (page + 1) + " of " + maxPage + ")");

        int start = page * perPage;
        int end = Math.min(start + perPage, totalShops);

        for (int i = start; i < end; i++) {
            PlayerShop shop = openShops.get(i);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.getOwner());

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(owner);
                meta.setDisplayName("§b" + owner.getName());
                meta.setLore(List.of("§7Click to view shop"));
                head.setItemMeta(meta);
            }

            gui.addItem(head);
        }

        // Pagination buttons
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§ePrevious Page");
        back.setItemMeta(backMeta);
        gui.setItem(45, back);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName("§eNext Page");
        next.setItemMeta(nextMeta);
        gui.setItem(53, next);

        // TODO:EMERALD and CHEST
        // Balance display in slot 48
        int balance = plugin.getTimeraldManager().get(player.getUniqueId());
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        emeraldMeta.setDisplayName("§aYour Balance: §b" + balance + " Timerald");
        emeraldMeta.setLore(List.of("§7Earn Timeralds by depositing emeralds"));
        emerald.setItemMeta(emeraldMeta);
        gui.setItem(48, emerald);

        // Stash
        ItemStack stash = new ItemStack(Material.CHEST);
        ItemMeta stashMeta = stash.getItemMeta();
        stashMeta.setDisplayName("§aStash");
        stashMeta.setLore(List.of("§eClick to open your stash"));
        stash.setItemMeta(stashMeta);
        gui.setItem(50, stash);

        player.openInventory(gui);
    }



    // ACTION: SHOP
    @EventHandler
    public void onClickShopPage(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        InventoryView view = event.getView();
        String title = view.getTitle();

        if (!title.startsWith(SHOP_GUI_PREFIX)) return;
        event.setCancelled(true);
        switch (event.getClick()) {
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
            case DOUBLE_CLICK:
            case NUMBER_KEY:
            case MIDDLE:
            case DROP:
            case CONTROL_DROP:
                event.setResult(org.bukkit.event.Event.Result.DENY);
                player.updateInventory();
                return;
        }

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Extract current page number
        int currentPage = 0;
        Matcher matcher = Pattern.compile("Page (\\d+)").matcher(title);
        if (matcher.find()) {
            try {
                currentPage = Integer.parseInt(matcher.group(1)) - 1;
            } catch (NumberFormatException ignored) {}
        }

        List<PlayerShop> openShops = shopManager.getOpenShops().stream()
            .filter(shop -> !shop.getListings().isEmpty())
            .toList();

        int perPage = 45;
        int maxPage = (int) Math.ceil((double) openShops.size() / perPage);

        // Pagination buttons
        if (slot == 45 && currentPage > 0) {
            openPlayerShopSelectorGUI(player, currentPage - 1);
            return;
        }

        // Clicked "Emerald"
        if (slot == 48) return;

        // Clicked "Stash"
        if (slot == 50) {
            PlayerShop shop = shopManager.getOrCreateShop(uuid);
            openStashGUI(player, shop);
            return;
        }

        if (slot == 53 && currentPage < maxPage - 1) {
            openPlayerShopSelectorGUI(player, currentPage + 1);
            return;
        }

        // Player head click → open that shop
        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                UUID ownerId = meta.getOwningPlayer().getUniqueId();
                PlayerShop shop = shopManager.getShop(ownerId);
                if (shop != null && !shop.getListings().isEmpty()) {
                    openPlayerShopGUI(player, ownerId, shop, 0);
                } else {
                    player.sendMessage("§cThat shop is empty or closed.");
                }
            }
        }
    }

    // UI: PLAYER SHOP
    public void openPlayerShopGUI(Player viewer, UUID ownerId, PlayerShop shop, int page) {
        String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        List<ShopItem> listings = shop.getListings();

        int itemsPerPage = 45;
        int maxPage = (int) Math.ceil((double) listings.size() / itemsPerPage);
        if (page < 0) page = 0;
        if (page >= maxPage) page = maxPage - 1;

        Inventory gui = Bukkit.createInventory(null, 54, PLAYER_SHOP_GUI_PREFIX + ownerName + " §8(Page " + (page + 1) + ")");

        // Display up to 45 items
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, listings.size());
        for (int i = start; i < end; i++) {
            ShopItem item = listings.get(i);
            ItemStack display = item.getItem().clone();
            ItemMeta meta = display.getItemMeta();

            if (meta != null) {
                int stock = getTotalMatchingItems(shop.getStash(), item.getItem());
                String stockLine;
                if (stock <= 0) {
                    stockLine = "§7Stock available: §cOut of stock";
                } else if (stock == 1) {
                    stockLine = "§7Stock available: §e1 §7item";
                } else {
                    stockLine = "§7Stock available: §e" + stock + " §7items";
                }

                String purchaseLine;
                if (ownerName != viewer.getName()) {
                    purchaseLine = "§8Click to purchase";
                } else {
                    purchaseLine = "§cClick to unlist";
                }

                meta.setLore(List.of(
                    "§7cost: §b" + item.getPrice() + " Timerald",
                    "§7Quantity per purchase: §ax" + item.getQuantity(),
                    stockLine,
                    "",
                    purchaseLine,
                    "§8Index: " + i
                ));
                display.setItemMeta(meta);
            }

            gui.setItem(i - start, display);
        }

        // Navigation buttons
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prev.getItemMeta();
        prevMeta.setDisplayName("§a« Previous Page");
        prev.setItemMeta(prevMeta);
        if (page > 0) gui.setItem(45, prev);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName("§aNext Page »");
        next.setItemMeta(nextMeta);
        if (page < maxPage - 1) gui.setItem(53, next);

        // Balance display in slot 49
        int balance = plugin.getTimeraldManager().get(viewer.getUniqueId());
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        emeraldMeta.setDisplayName("§aYour Balance: §b" + balance + " Timerald");
        emeraldMeta.setLore(List.of("§7Earn Timeralds by depositing emeralds","§cClick to return to Shop list"));
        emerald.setItemMeta(emeraldMeta);
        gui.setItem(49, emerald);

        viewer.openInventory(gui);
    }


    // ACTION: PLAYER SHOP (Purchase)
    @EventHandler
    public void onPlayerShopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        String title = event.getView().getTitle();
        if (!title.startsWith(PLAYER_SHOP_GUI_PREFIX)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Extract shop owner name and page
        Matcher matcher = Pattern.compile(Pattern.quote(PLAYER_SHOP_GUI_PREFIX) + "(.+?) §8\\(Page (\\d+)\\)").matcher(title);
        if (!matcher.find()) return;

        String ownerName = matcher.group(1);
        int page;
        try {
            page = Integer.parseInt(matcher.group(2)) - 1;
        } catch (NumberFormatException e) {
            return;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);
        PlayerShop shop = shopManager.getShop(owner.getUniqueId());
        String sellerName = owner.getName();

        // Pagination logic
        int slot = event.getSlot();
        int perPage = 45;
        int index = page * perPage + slot;

        // Clicked "Previous"
        if (slot == 45 && page > 0) {
            if (shop != null) {
                openPlayerShopGUI(player, owner.getUniqueId(), shop, page - 1);
            }
            return;
        }

        // Clicked "Next"
        if (slot == 53) {
            if (shop != null) {
                int maxPage = (int) Math.ceil((double) shop.getListings().size() / perPage);
                if (page + 1 < maxPage) {
                    openPlayerShopGUI(player, owner.getUniqueId(), shop, page + 1);
                }
            }
            return;
        }

        // Clicked "Emerald"
        if (slot == 49) {
            openPlayerShopSelectorGUI(player, 0);
            return;
        }

        if (recentlyClicked.contains(uuid)) {
            // player.sendMessage("§ePlease wait before making another purchase.");
            return;
        }
        // Otherwise, clicked a shop item — simulate /shop buy
        if (sellerName.equals(player.getName())) {
            player.performCommand("shop cancel " + index);
        } else {
            player.performCommand("shop buy " + ownerName + " " + index);
        }
        recentlyClicked.remove(uuid);
        player.closeInventory();
    }

    // Helper:
    private int getTotalMatchingItems(List<ItemStack> stash, ItemStack target) {
        int total = 0;
        for (ItemStack item : stash) {
            if (item != null && item.isSimilar(target)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public void broadcastListing(Player sender, ItemStack item, double price, int quantity) {
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
        ? item.getItemMeta().getDisplayName()
        : formatMaterialName(item.getType());

        // Build message with legacy color codes (no NamedTextColor used)
        Component message = Component.text("§b⬥ §f" + sender.getName() + "§7 is selling §a" + itemName +
                " §7for §b" + price + " Timerald §7(x" + quantity + ") ")
            .append(Component.text("§e[Open Shop]")
                .clickEvent(ClickEvent.runCommand("/shop visit " + sender.getName())));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(sender)) {
                player.sendMessage(message);
            }
        }
    }

    public String formatMaterialName(Material material) {
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
