package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PreventPlacementListener implements Listener {

    private final Timerald plugin;

    public PreventPlacementListener(Timerald plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.getType() != Material.SOUL_LANTERN) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop.luminous-lantern");
        String expected = section.getString("name");
        if (expected.equals(meta.getDisplayName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis item cannot be placed.");
        }
    }
}
