package com.darkun7.timerald.command;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.TimeraldManager;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class WithdrawCommand implements CommandExecutor {

    private final Timerald plugin;

    public WithdrawCommand(Timerald plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can withdraw.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /withdraw <amount (max 64)>");
            return true;
        }

        int requested;
        try {
            requested = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number.");
            return true;
        }

        if (requested < 1 || requested > 64) {
            player.sendMessage("§cWithdraw amount must be between 1 and 64.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        TimeraldManager manager = plugin.getTimeraldManager();
        int balance = manager.get(uuid);

        if (balance < requested) {
            player.sendMessage("§cYou only have §b" + balance + " Timerald§c.");
            return true;
        }

        // Create item: prefer emerald blocks if divisible by 9
        ItemStack result = (requested % 9 == 0)
                ? new ItemStack(Material.EMERALD_BLOCK, requested / 9)
                : new ItemStack(Material.EMERALD, requested);

        PlayerInventory inventory = player.getInventory();
        int emptySlot = inventory.firstEmpty();

        if (emptySlot == -1) {
            player.sendMessage("§cYour inventory is full.");
            return true;
        }

        inventory.setItem(emptySlot, result);
        manager.subtract(uuid, requested);
        player.sendMessage("§aWithdrew §b" + requested + " Timerald§a.");
        return true;
    }
}
