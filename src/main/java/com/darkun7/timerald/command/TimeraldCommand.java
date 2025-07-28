package com.darkun7.timerald.command;

import com.darkun7.limiter.PlayTimeLimiter;
import com.darkun7.limiter.api.PlayTimeLimiterAPI;
import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.TimeraldManager;
import com.darkun7.timerald.gui.TimeraldShopGUI;
import com.darkun7.timerald.listener.ClickChainListener;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class TimeraldCommand implements CommandExecutor {

    private final Timerald plugin;
    private final ClickChainListener quickaction;

    public TimeraldCommand(Timerald plugin, ClickChainListener quickaction) {
        this.plugin = plugin;
        this.quickaction = quickaction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        TimeraldManager manager = plugin.getTimeraldManager();
        // /timerald top
        if (args.length == 1 && args[0].equalsIgnoreCase("top")) {
            List<Map.Entry<UUID, Integer>> topList = manager.getTopTimeralds(10);

            player.sendMessage("§eTop Timerald Holders:");
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : topList) {
                UUID topUUID = entry.getKey();
                int balance = entry.getValue();

                // Resolve alias if necessary
                UUID resolved = manager.resolve(topUUID);
                OfflinePlayer offline = Bukkit.getOfflinePlayer(resolved);
                String name = offline.getName() != null ? offline.getName() : "Unknown";

                player.sendMessage("§e" + rank++ + ". §f" + name + " §7- §b" + balance + " Timerald");
            }

            return true;
        }

        // /timerald quickaction
        if (args.length == 1 && args[0].equalsIgnoreCase("quickaction")) {
            boolean nowEnabled = quickaction.toggleCombo(uuid);
            player.sendMessage("§7Click combo is now " + (nowEnabled ? "§aenabled" : "§cdisabled"));
            return true;
        }


        // /timerald send <target> <amount>
        if (args.length == 3 && args[0].equalsIgnoreCase("send")) {
            String targetName = args[1];
            int amount;

            if (targetName.equalsIgnoreCase(player.getName())) {
                player.sendMessage("§cYou cannot send Timerald to yourself.");
                return true;
            }

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount.");
                return true;
            }

            if (amount <= 0) {
                player.sendMessage("§cAmount must be greater than 0.");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage("§cThat player has never joined before.");
                return true;
            }

            if (!manager.subtract(uuid, amount)) {
                player.sendMessage("§cNot enough Timerald.");
                return true;
            }

            manager.add(target.getUniqueId(), amount);
            player.sendMessage("§aSent §b" + amount + " Timerald §ato §f" + target.getName());
            if (target.isOnline()) {
                ((Player) target).sendMessage("§b" + player.getName() + " §asent you §b" + amount + " Timerald§a.");
            }
            return true;
        }

        // Default: shop view

        new TimeraldShopGUI(plugin).open(player);

        return true;
    }
}