package com.darkun7.timerald.shop;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.TimeraldManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ShopManager {

    private final Timerald plugin;
    private final Map<UUID, PlayerShop> shopMap = new HashMap<>();
    private final File shopFolder;
    private final TimeraldManager timeraldManager;

    public ShopManager(Timerald plugin, TimeraldManager timeraldManager) {
        this.plugin = plugin;
        this.timeraldManager = timeraldManager;
        this.shopFolder = new File(plugin.getDataFolder(), "shops");
        if (!shopFolder.exists()) shopFolder.mkdirs();
    }
    
    public PlayerShop getShop(UUID uuid) {
        return shopMap.get(timeraldManager.resolve(uuid));
    }

    public PlayerShop getOrCreateShop(UUID uuid) {
        UUID resolved = timeraldManager.resolve(uuid);
        return shopMap.computeIfAbsent(resolved, PlayerShop::new);
    }

    public PlayerShop openShop(Player player) {
        UUID uuid = timeraldManager.resolve(player.getUniqueId());
        PlayerShop shop = new PlayerShop(uuid);
        shopMap.put(uuid, shop);
        return shop;
    }

    public Collection<PlayerShop> getOpenShops() {
        return shopMap.values();
    }

    public void savePlayerShop(UUID uuid) {
        UUID resolved = timeraldManager.resolve(uuid);
        PlayerShop shop = shopMap.get(resolved);
        if (shop == null) return;

        File file = new File(shopFolder, resolved.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Save listings
        List<Map<String, Object>> listingData = new ArrayList<>();
        for (ShopItem item : shop.getListings()) {
            listingData.add(item.serialize());
        }
        config.set("owner", resolved.toString());
        config.set("listings", listingData);

        // Save stash
        List<Map<String, Object>> stashData = new ArrayList<>();
        for (ItemStack item : shop.getStash()) {
            stashData.add(item.serialize());
        }
        config.set("stash", stashData);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save shop for: " + uuid, e);
        }
    }


    public void saveShops() {
        for (Map.Entry<UUID, PlayerShop> entry : shopMap.entrySet()) {
            UUID uuid = entry.getKey();
            UUID resolved = timeraldManager.resolve(uuid);
            PlayerShop shop = entry.getValue();
            File file = new File(shopFolder, resolved.toString() + ".yml");
            YamlConfiguration config = new YamlConfiguration();

            // Save listings
            List<Map<String, Object>> listingData = new ArrayList<>();
            for (ShopItem item : shop.getListings()) {
                listingData.add(item.serialize());
            }

            config.set("owner", resolved.toString());
            config.set("listings", listingData);

            // Save stash manually as a section
            List<Map<String, Object>> stashData = new ArrayList<>();
            for (ItemStack item : shop.getStash()) {
                stashData.add(item.serialize());
            }
            config.set("stash", stashData);

            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save shop for: " + uuid, e);
            }
        }
    }

    public void loadShops() {
        if (!shopFolder.exists()) return;

        for (File file : Objects.requireNonNull(shopFolder.listFiles((dir, name) -> name.endsWith(".yml")))) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            try {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                UUID resolved = timeraldManager.resolve(uuid);
                PlayerShop shop = new PlayerShop(resolved);

                // Load listings
                List<Map<?, ?>> listingData = config.getMapList("listings");
                for (Map<?, ?> map : listingData) {
                    shop.addListing(ShopItem.deserialize(map));
                }

                // Load stash manually
                List<Map<?, ?>> stashData = config.getMapList("stash");
                for (Map<?, ?> map : stashData) {
                    ItemStack item = ItemStack.deserialize((Map<String, Object>) map);
                    shop.addToStash(item);
                }

                shopMap.put(resolved, shop);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load shop from file: " + file.getName(), e);
            }
        }
    }
}
