package com.darkun7.timerald.gui;

import com.darkun7.limiter.PlayTimeLimiter;
import com.darkun7.limiter.api.PlayTimeLimiterAPI;
import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.TimeraldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class TimeraldShopGUI implements Listener {

    private final Timerald plugin;
    private final NamespacedKey shopItemKey;

    public TimeraldShopGUI(Timerald plugin) {
        this.plugin = plugin;
        this.shopItemKey = new NamespacedKey(plugin, "shop-item");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§bTimerald Shop");
        TimeraldManager manager = plugin.getTimeraldManager();
        ConfigurationSection shop = plugin.getConfig().getConfigurationSection("shop");
        if (shop == null) return;

        UUID uuid = player.getUniqueId();
        int balance = manager.get(uuid);

        // Balance display in slot 4
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        emeraldMeta.setDisplayName("§aYour Balance: §b" + balance + " Timerald");
        emeraldMeta.setLore(List.of("§7Earn Timeralds by depositing emeralds"));
        emerald.setItemMeta(emeraldMeta);
        gui.setItem(4, emerald);

        // Playtime options (slots 10-16)
        ConfigurationSection playtime = shop.getConfigurationSection("playtime");
        int[] playtimeSlots = {10, 11, 12, 13, 14, 15, 16};
        int i = 0;
        if (playtime != null) {
            for (String key : playtime.getKeys(false)) {
                if (i >= playtimeSlots.length) break;
                try {
                    int minutes = Integer.parseInt(key);
                    int cost = playtime.getInt(key, -1);
                    if (cost < 0) continue;

                    ItemStack item = new ItemStack(Material.CLOCK);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("§eBuy " + minutes + " Minutes");
                    meta.setLore(List.of("§7Cost: §b" + cost + " Timerald", "§8Click to purchase"));
                    meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.INTEGER, 1);
                    item.setItemMeta(meta);
                    gui.setItem(playtimeSlots[i++], item);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Elixir in slot 22
        if (shop.contains("elixir")) {
            ConfigurationSection elixir = shop.getConfigurationSection("elixir");
            int cost = elixir.getInt("cost", -1);
            if (cost >= 0) {
                Material material = Material.getMaterial(elixir.getString("material", "POTION"));
                String name = elixir.getString("name", "§dTime Elixir");
                List<String> lore = elixir.getStringList("lore");

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(List.of("§7Cost: §b" + cost + " Timerald", "§8Click to purchase Elixir"));
                meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.INTEGER, 1);
                item.setItemMeta(meta);
                gui.setItem(22, item);
            }
        }

        // Decorative filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int slot = 0; slot < 27; slot++) {
            if (gui.getItem(slot) == null) {
                gui.setItem(slot, filler);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("§bTimerald Shop")) return;
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        UUID uuid = player.getUniqueId();
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // ✅ Only process if it's a valid shop item
        if (!container.has(shopItemKey, PersistentDataType.INTEGER)) return;

        // ✅ Remove the tag to prevent re-processing
        container.remove(shopItemKey);
        clicked.setItemMeta(meta);
        player.closeInventory();

        // Delay processing to ensure GUI is fully closed
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TimeraldManager manager = plugin.getTimeraldManager();
            String name = meta.getDisplayName();

            if (name.startsWith("§eBuy ") && name.contains("Minutes")) {
                try {
                    int minutes = Integer.parseInt(name.split(" ")[1]);
                    ConfigurationSection playtimeSection = plugin.getConfig().getConfigurationSection("shop.playtime");
                    if (playtimeSection == null) return;
                    int cost = playtimeSection.getInt(String.valueOf(minutes), -1);
                    if (cost < 0) {
                        player.sendMessage("§cInvalid item configuration.");
                        return;
                    }

                    if (manager.get(uuid) < cost) {
                        player.sendMessage("§cNot enough Timerald.");
                        return;
                    }

                    manager.subtract(uuid, cost);
                    PlayTimeLimiterAPI api = PlayTimeLimiter.getInstance().getAPI();
                    api.reduceDailyUsed(uuid, minutes);
                    player.sendMessage("§aPurchased §f" + minutes + " minute(s) §afor §b" + cost + " Timerald.");
                } catch (NumberFormatException ignored) {}
                return;
            }

            // Handle elixir
            ConfigurationSection elixirSection = plugin.getConfig().getConfigurationSection("shop.elixir");
            if (elixirSection != null && name.equals(elixirSection.getString("name"))) {
                int cost = elixirSection.getInt("cost", -1);
                if (cost < 0) {
                    player.sendMessage("§cInvalid elixir configuration.");
                    return;
                }

                if (manager.get(uuid) < cost) {
                    player.sendMessage("§cNot enough Timerald.");
                    return;
                }

                Material mat = Material.getMaterial(elixirSection.getString("material", "POTION"));
                ItemStack item = new ItemStack(mat);
                ItemMeta elixirMeta = item.getItemMeta();
                elixirMeta.setDisplayName(elixirSection.getString("name"));
                elixirMeta.setLore(elixirSection.getStringList("lore"));
                item.setItemMeta(elixirMeta);

                manager.subtract(uuid, cost);
                player.getInventory().addItem(item);
                player.sendMessage("§aPurchased §dTime Elixir §afor §b" + cost + " Timerald.");
            }
        }, 1L);
    }
}
