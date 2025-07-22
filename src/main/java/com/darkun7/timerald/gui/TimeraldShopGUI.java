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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;
import java.util.stream.Collectors;

import java.util.List;
import java.util.UUID;

public class TimeraldShopGUI implements Listener {

    private final Timerald plugin;
    private final NamespacedKey shopItemKey;
    public static final List<String> ITEM_KEYS = List.of(
            "luminous-lantern",
            "time-elixir",
            "playerhead",
            "smoke-bomb",
            "nymph-snack"
        );

    public TimeraldShopGUI(Timerald plugin) {
        this.plugin = plugin;
        this.shopItemKey = new NamespacedKey(plugin, "shop-item");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§bTimerald §aShop");
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

                ConfigurationSection entry = playtime.getConfigurationSection(key);
                if (entry == null) continue;

                int cost = entry.getInt("cost", -1);
                int minutes = entry.getInt("minutes", -1);
                if (cost < 0 || minutes <= 0) continue;

                String materialName = entry.getString("material", "CLOCK");
                Material material = Material.matchMaterial(materialName);
                if (material == null) material = Material.CLOCK;

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                            entry.getString("name", minutes + " Minutes")));

                    List<String> lore = entry.getStringList("lore");
                    if (lore != null) {
                        meta.setLore(lore.stream()
                                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                                .collect(Collectors.toList()));
                    }
                    meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.INTEGER, 1);
                    
                    int amount = Math.min(minutes, 64);
                    if (amount <= 0) amount = 1;
                    item.setAmount(amount);
                    item.setItemMeta(meta);
                }

                gui.setItem(playtimeSlots[i++], item);
            }

        }

        // Configurable item handler
        for (String key : ITEM_KEYS) {
            if (shop.contains(key)) {
                ConfigurationSection section = shop.getConfigurationSection(key);
                int position = section.getInt("position", 21);
                int cost = section.getInt("cost", -1);
                if (cost < 0) continue;
                String matName = section.getString("material", "PAPER");
                Material material = Material.getMaterial(matName.toUpperCase());
                if (material == null) material = Material.PAPER;

                String name = section.getString("name", "§fItem");
                List<String> lore = section.getStringList("lore");

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(List.of("§7Cost: §b" + cost + " Timerald", "§8Click to purchase"));
                meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.INTEGER, 1);
                item.setItemMeta(meta);

                gui.setItem(position, item);
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
        if (!e.getView().getTitle().equals("§bTimerald §aShop")) return;
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        int slot = e.getSlot();
        // Bukkit.getLogger().info("[DEBUG] Clicked slot: " + slot);

        UUID uuid = player.getUniqueId();
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(shopItemKey, PersistentDataType.INTEGER)) return;

        container.remove(shopItemKey);
        clicked.setItemMeta(meta);
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TimeraldManager manager = plugin.getTimeraldManager();
            ConfigurationSection shop = plugin.getConfig().getConfigurationSection("shop");
            if (shop == null) return;
            String name = meta.getDisplayName();

            ConfigurationSection playtimeSection = shop.getConfigurationSection("playtime");
            if (playtimeSection == null) return;

            for (String key : playtimeSection.getKeys(false)) {
                ConfigurationSection entry = playtimeSection.getConfigurationSection(key);
                if (entry == null) continue;

                int position = entry.getInt("position", -1);
                if (position != slot) continue;

                int cost = entry.getInt("cost", -1);
                int minutes = entry.getInt("minutes", -1);
                if (cost < 0 || minutes < 0) {
                    player.sendMessage("§cInvalid item configuration.");
                    return;
                }
                
                String displayName = entry.getString("name");

                if (position != slot) continue;
                if (manager.get(uuid) < cost) {
                    player.sendMessage("§cNot enough Timerald.");
                    return;
                }

                manager.subtract(uuid, cost);
                PlayTimeLimiterAPI api = PlayTimeLimiter.getInstance().getAPI();
                api.reduceDailyUsed(uuid, minutes);
                player.sendMessage("§aPurchased §f" + minutes + " minute(s) §afor §b" + cost + " Timerald.");
                return;
            }

            for (String key : ITEM_KEYS) {
                ConfigurationSection section = shop.getConfigurationSection(key);
                if (section == null || !name.equals(section.getString("name"))) continue;

                int cost = section.getInt("cost", -1);
                if (cost < 0) {
                    player.sendMessage("§cInvalid configuration.");
                    return;
                }

                if (manager.get(uuid) < cost) {
                    player.sendMessage("§cNot enough Timerald.");
                    return;
                }

                String matName = section.getString("material", "PAPER");
                Material mat = Material.getMaterial(matName.toUpperCase());
                if (mat == null) mat = Material.PAPER;

                ItemStack item = new ItemStack(mat);
                ItemMeta itemMeta = item.getItemMeta();
                itemMeta.setDisplayName(section.getString("name"));
                itemMeta.setLore(section.getStringList("lore"));

                // Special case: Player Head
                if (mat == Material.PLAYER_HEAD) {
                    SkullMeta skullMeta = (SkullMeta) itemMeta;
                    skullMeta.setOwningPlayer(player);
                    skullMeta.setDisplayName("§f" + player.getName() + "'s Head");
                    item.setItemMeta(skullMeta);
                } else {
                    item.setItemMeta(itemMeta);
                }


                item.setItemMeta(itemMeta);
                manager.subtract(uuid, cost);
                player.getInventory().addItem(item);
                player.sendMessage("§aPurchased §f" + section.getString("name") + " §afor §b" + cost + " Timerald.");
            }
        }, 1L);
    }
}
