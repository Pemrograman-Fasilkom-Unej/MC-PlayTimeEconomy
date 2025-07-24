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

            // Check if buyer has space in inventory
            ItemStack toGive = item.getItem().clone();
            toGive.setAmount(item.getQuantity());

            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(toGive.clone());
            if (!remaining.isEmpty()) {
                player.sendMessage("§cNot enough inventory space to complete the purchase.");
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

        player.openInventory(gui);
    }



    // click Shop
    @EventHandler
    public void onClickShopPage(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryView view = event.getView();
        String title = view.getTitle();

        if (!title.startsWith(SHOP_GUI_PREFIX)) return;
        event.setCancelled(true);

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

                meta.setLore(List.of(
                    "§7Price: §f" + item.getPrice() + " §2Timerald",
                    "§7Quantity per purchase: §ax" + item.getQuantity(),
                    "§7Stock available: §e" + stock + " items",
                    "",
                    "§eClick to buy",
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
        emeraldMeta.setLore(List.of("§7Earn Timeralds by depositing emeralds"));
        emerald.setItemMeta(emeraldMeta);
        gui.setItem(49, emerald);

        viewer.openInventory(gui);
    }


    // Purchase
    @EventHandler
    public void onPlayerShopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

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

        // Pagination logic
        int slot = event.getSlot();
        int perPage = 45;
        int index = page * perPage + slot;

        // Clicked "Previous"
        if (slot == 45 && page > 0) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);
            PlayerShop shop = shopManager.getShop(owner.getUniqueId());
            if (shop != null) {
                openPlayerShopGUI(player, owner.getUniqueId(), shop, page - 1);
            }
            return;
        }

        // Clicked "Next"
        if (slot == 53) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);
            PlayerShop shop = shopManager.getShop(owner.getUniqueId());
            if (shop != null) {
                int maxPage = (int) Math.ceil((double) shop.getListings().size() / perPage);
                if (page + 1 < maxPage) {
                    openPlayerShopGUI(player, owner.getUniqueId(), shop, page + 1);
                }
            }
            return;
        }

        if (slot == 49) return;

        UUID uuid = player.getUniqueId();

        if (recentlyClicked.contains(uuid)) {
            // player.sendMessage("§ePlease wait before making another purchase.");
            return;
        }
        // Otherwise, clicked a shop item — simulate /shop buy
        player.performCommand("shop buy " + ownerName + " " + index);
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

}
