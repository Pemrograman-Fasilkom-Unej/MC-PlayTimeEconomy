package com.darkun7.timerald.command;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.TimeraldManager;
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

        ItemStack inHand = player.getInventory().getItemInMainHand();

        UUID uuid = player.getUniqueId();
        int amount = inHand.getAmount();
        int value = 0;

        if (inHand.getType() == Material.EMERALD) value = amount;
        else if (inHand.getType() == Material.EMERALD_BLOCK) value = amount * 9;
        else {
            player.sendMessage("§cOnly emeralds or emerald blocks are accepted.");
            return true;
        }

        plugin.getTimeraldManager().add(uuid, value);
        player.getInventory().setItemInMainHand(null);
        player.sendMessage("§aDeposited §f" + value + " Timerald§a.");
        return true;
    }
}
