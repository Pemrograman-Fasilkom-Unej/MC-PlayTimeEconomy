package com.darkun7.timerald.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.darkun7.timerald.shop.ShopItem;
import com.darkun7.timerald.shop.PlayerShop;
import com.darkun7.timerald.shop.ShopManager;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

public class ShopTabCompleter implements TabCompleter {
    
    private final ShopManager shopManager;

    public ShopTabCompleter(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("stash", "sell", "cancel", "visit");
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            completions.addAll(Arrays.asList("10", "25", "50", "100"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("sell")) {
            completions.addAll(Arrays.asList("@1", "@16", "@32", "@64"));
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            PlayerShop targetShop = shopManager.getShop(uuid);
            List<ShopItem> listings = targetShop.getListings();
            for (int i = 0; i < listings.size(); i++) {
                completions.add(String.valueOf(i));
            }
            // PlayerShop shop = YourPlugin.getInstance().getShopManager().getShop(uuid);

            // if (shop != null) {
            //     for (int i = 0; i < shop.getListings().size(); i++) {
            //         completions.add(String.valueOf(i));
            //     }
            // }
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("visit")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(online.getName());
                }
            }
        }

        // else if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
        //     for (Player online : Bukkit.getOnlinePlayers()) {
        //         if (online.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
        //             completions.add(online.getName());
        //         }
        //     }
        // }

        return completions;
    }
}
