package com.darkun7.timerald.gui;

import com.darkun7.timerald.util.Helpers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.shop.ShopItem;
import com.darkun7.timerald.shop.OrderItem;
import com.darkun7.timerald.shop.OrderEntry;
import com.darkun7.timerald.shop.ShopManager;
import com.darkun7.timerald.shop.PlayerShop;
import com.darkun7.timerald.util.Helpers;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopGUI implements Listener {

    private final Timerald plugin;
    private final ShopManager shopManager;

    public ShopGUI(Timerald plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private static final String SHOP_GUI_PREFIX = "§2Shops §8(";
    private static final String PLAYER_SHOP_GUI_PREFIX = "§2Shop: §1";
    private static final String REQUEST_GUI_PREFIX = "§2Shop Requests §8- #"; 
    // private static final String PLAYER_REQUEST_GUI_PREFIX = "§2Requests §8(Page ";
    private final Set<UUID> recentlyClicked = new HashSet<>();

    // UI: STASH  
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
    public void openPlayerShopSelectorGUI(Player player, int page) {
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
                meta.setLore(List.of("§eClick to view shop"));
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

        // Balance display in slot 48
        int balance = plugin.getTimeraldManager().get(player.getUniqueId());
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        emeraldMeta.setDisplayName("§aYour Balance: §b" + balance + " Timerald");
        emeraldMeta.setLore(List.of("§7Earn Timeralds by depositing emeralds"));
        emerald.setItemMeta(emeraldMeta);
        gui.setItem(48, emerald);

        // Request in slot 49
        ItemStack composter = new ItemStack(Material.COMPOSTER);
        ItemMeta composterMeta = composter.getItemMeta();
        composterMeta.setDisplayName("§aRequest");
        composterMeta.setLore(List.of("§7On-demand item"));
        composterMeta.setLore(List.of("§eClick to open requested items"));
        composter.setItemMeta(composterMeta);
        gui.setItem(49, composter);

        // Stash
        ItemStack stash = new ItemStack(Material.ENDER_CHEST);
        ItemMeta stashMeta = stash.getItemMeta();
        stashMeta.setDisplayName("§aStash");
        stashMeta.setLore(List.of("§7As vault to store item to sell"));
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

        // Clicked "Composter"
        if (slot == 49) {
            openRequestsUI(player, 0);
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
                int stock = Helpers.getTotalMatchingItems(shop.getStash(), item.getItem());
                String stockLine;
                int amount = 1;
                if (stock <= 0) {
                    stockLine = "§7Stock available: §cOut of stock";
                } else if (stock == 1) {
                    stockLine = "§7Stock available: §e1 §7item";
                } else {
                    amount = Math.min(stock/item.getQuantity(), 64);
                    if (amount <= 0) amount = 1;
                    stockLine = "§7Stock available: §e" + stock + " §7items";
                }

                String purchaseLine;
                if (ownerName != viewer.getName()) {
                    purchaseLine = "§bClick to purchase";
                } else {
                    purchaseLine = "§cClick to unlist";
                }

                meta.setLore(List.of(
                    "§7Cost: §b" + item.getPrice() + " Timerald",
                    "§7Quantity per purchase: §ax" + item.getQuantity(),
                    stockLine,
                    "",
                    purchaseLine,
                    "§8Index: " + i
                ));
                display.setItemMeta(meta);
                display.setAmount(amount);
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

        // Balance display in slot 48
        int balance = plugin.getTimeraldManager().get(viewer.getUniqueId());
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        emeraldMeta.setDisplayName("§aYour Balance: §b" + balance + " Timerald");
        emeraldMeta.setLore(List.of("§7Earn Timeralds by depositing emeralds","§eClick to return to Shop list"));
        emerald.setItemMeta(emeraldMeta);
        gui.setItem(48, emerald);

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
        List<String> lore = clicked.getItemMeta().getLore();
        if (lore == null || lore.size() < 2) return;

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
        if (slot == 48) {
            openPlayerShopSelectorGUI(player, 0);
            return;
        }

        // if (recentlyClicked.contains(uuid)) {
        //     // player.sendMessage("§ePlease wait before making another purchase.");
        //     return;
        // }
        // Otherwise, clicked a shop item — simulate /shop buy
        if (sellerName.equals(player.getName())) {
            player.performCommand("shop cancel " + index);
            player.closeInventory();
        } else {
            player.performCommand("shop buy " + ownerName + " " + index);
            openPlayerShopGUI(player, owner.getUniqueId(), shop, 0);
        }
        // recentlyClicked.remove(uuid);
    }

    // UI: REQUESTS
    public void openRequestsUI(Player viewer, int page) {
        UUID uuid = viewer.getUniqueId();
        int pageSize = 45; // 54 slots, last row for navigation
        List<OrderEntry> allRequests = shopManager.getAllRequests();

        // No requests
        if (allRequests.isEmpty()) {
            Inventory emptyGui = Bukkit.createInventory(null, 54, REQUEST_GUI_PREFIX + "1");
            ItemStack noData = new ItemStack(Material.BARRIER);
            ItemMeta noDataMeta = noData.getItemMeta();
            noDataMeta.setDisplayName("§cNo requests available");
            noDataMeta.setLore(List.of("§7Try §6/shop order §7to request item","§eClick to return to Shop list"));
            noData.setItemMeta(noDataMeta);
            emptyGui.setItem(22, noData);
            viewer.openInventory(emptyGui);
            return;
        }

        int maxPage = (int) Math.ceil((double) allRequests.size() / pageSize);
        if (maxPage <= 0) maxPage = 1;
        page = Math.max(0, Math.min(page, maxPage - 1));

        Inventory gui = Bukkit.createInventory(null, 54, REQUEST_GUI_PREFIX + (page + 1));

        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allRequests.size());

        for (int i = startIndex; i < endIndex; i++) {
            OrderEntry entry = allRequests.get(i);
            OrderItem orderItem = entry.getOrderItem();
            ItemStack item = orderItem.getItem().clone();
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                String purchaseLine;
                if (entry.getRequesterName() != viewer.getName()) {
                    purchaseLine = "§3Click to deliver";
                } else {
                    purchaseLine = "§cClick to unlist";
                }

                meta.setLore(List.of(
                    "§7Requester: §e" + entry.getRequesterName(),
                    "§7Price: §b" + orderItem.getPrice() + " Timerald",
                    "§7Quantity per order: §ax" + orderItem.getQuantity(),
                    "§7Limit: §ax" + orderItem.getLimit(),
                    "",
                    purchaseLine,
                    "§8Index: " + entry.getIndex(),
                    "§8[Requested Item]"
                ));
                meta.setDisplayName("§a" + Helpers.formatMaterialName(item.getType()));
                item.setAmount(orderItem.getLimit());
                item.setItemMeta(meta);
            }
            gui.addItem(item);
        }

        // Prev arrow
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("§a« Previous Page");
            prev.setItemMeta(prevMeta);
            gui.setItem(45, prev);
        }

        // Next arrow
        if (page < maxPage - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§aNext Page »");
            next.setItemMeta(nextMeta);
            gui.setItem(53, next);
        }

        // Balance display in slot 48
        int balance = plugin.getTimeraldManager().get(uuid);
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        emeraldMeta.setDisplayName("§aYour Balance: §b" + balance + " Timerald");
        emeraldMeta.setLore(List.of("§7Earn Timeralds by depositing emeralds","§eClick to return to Shop list"));
        emerald.setItemMeta(emeraldMeta);
        gui.setItem(48, emerald);

        // Stash
        ItemStack stash = new ItemStack(Material.ENDER_CHEST);
        ItemMeta stashMeta = stash.getItemMeta();
        stashMeta.setDisplayName("§aStash");
        stashMeta.setLore(List.of("§7As vault to store item to sell"));
        stashMeta.setLore(List.of("§eClick to open your stash"));
        stash.setItemMeta(stashMeta);
        gui.setItem(50, stash);

        viewer.openInventory(gui);
    }

    // ACTION: REQUEST
    @EventHandler
    public void onRequestClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (event.getClickedInventory() == null || !event.getView().getTitle().startsWith(REQUEST_GUI_PREFIX)) return;
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

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        List<String> lore = clicked.getItemMeta().getLore();

        // Strip § manually
        String rawTitle = event.getView().getTitle().replaceAll("§.", "");

        // Parse current page directly
        int page;
        try {
            page = Integer.parseInt(rawTitle.replace(REQUEST_GUI_PREFIX, "")) - 1;
        } catch (NumberFormatException ex) {
            page = 0;
        }

        // Navigation arrows
        int slot = event.getSlot();
        // String displayName = clicked.getItemMeta().getDisplayName().replaceAll("§.", "");
        
        if (slot == 45 && page > 0) {
            openRequestsUI(player, page - 1);
            return;
        }
        if (slot == 53) {
            openRequestsUI(player, page + 1);
            return;
        }

        // Clicked "Emerald"
        if (slot == 48 || (slot == 22 && clicked.getType() == Material.BARRIER)) {
            openPlayerShopSelectorGUI(player, 0);
            return;
        }

        // Clicked "Stash"
        if (slot == 50) {
            PlayerShop shop = shopManager.getOrCreateShop(uuid);
            openStashGUI(player, shop);
            return;
        }

        // Check for [Requested Item] marker
        if (!lore.get(7).equals("§8[Requested Item]")) return;

        String requesterName = lore.get(0).replace("§7Requester: §e", "");
        int index;
        try {
            index = Integer.parseInt(lore.get(6).replace("§8Index: ", ""));
        } catch (NumberFormatException ex) {
            return;
        }

        OfflinePlayer requester = Bukkit.getOfflinePlayer(requesterName);
        if (!requester.getUniqueId().equals(player.getUniqueId())) {
            player.performCommand("shop fulfill " + requesterName + " " + index);
        } else {
            player.performCommand("shop cancel-order " + index);
        }
        openRequestsUI(player, page);
    }


}