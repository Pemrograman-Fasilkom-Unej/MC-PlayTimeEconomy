package com.darkun7.timerald.command;

import com.darkun7.timerald.Timerald;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DepositCommand implements CommandExecutor {

    private final Timerald plugin;

    public DepositCommand(Timerald plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can deposit.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        int emeralds = 0;
        int emeraldBlocks = 0;

        // Loop through inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);

            if (item == null || item.getType() == Material.AIR) continue;

            if (item.getType() == Material.EMERALD) {
                emeralds += item.getAmount();
                player.getInventory().clear(i);
            } else if (item.getType() == Material.EMERALD_BLOCK) {
                emeraldBlocks += item.getAmount();
                player.getInventory().clear(i);
            }
        }

        int totalTimerald = emeralds + (emeraldBlocks * 9);
        if (totalTimerald == 0) {
            player.sendMessage("§cNo emeralds or emerald blocks to deposit.");
            return true;
        }

        plugin.getTimeraldManager().add(uuid, totalTimerald);
        player.sendMessage("§aDeposited §b" + totalTimerald + " Timerald§a (" +
                emeralds + " emeralds, " + emeraldBlocks + " emerald blocks).");
        return true;
    }
}
