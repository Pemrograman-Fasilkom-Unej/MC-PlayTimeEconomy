package com.darkun7.timerald.command;

import com.darkun7.limiter.PlayTimeLimiter;
import com.darkun7.limiter.api.PlayTimeLimiterAPI;
import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.data.TimeraldManager;
import com.darkun7.timerald.gui.TimeraldShopGUI;

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

import java.util.List;
import java.util.UUID;

public class TimeraldCommand implements CommandExecutor {

    private final Timerald plugin;

    public TimeraldCommand(Timerald plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        TimeraldManager manager = plugin.getTimeraldManager();

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

        if (args.length == 3 && args[0].equalsIgnoreCase("buy")) {
            String category = args[1];
            String key = args[2];

            ConfigurationSection shop = plugin.getConfig().getConfigurationSection("shop");
            if (shop == null || !shop.contains(category)) {
                player.sendMessage("§cShop category not found.");
                return true;
            }

            if (category.equalsIgnoreCase("playtime")) {
                int minutes;
                try {
                    minutes = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number.");
                    return true;
                }

                int cost = shop.getConfigurationSection("playtime").getInt(key, -1);
                if (cost == -1) {
                    player.sendMessage("§cThis playtime option is not available.");
                    return true;
                }

                if (manager.get(uuid) < cost) {
                    player.sendMessage("§cNot enough Timerald. You need §b" + cost + "§c.");
                    return true;
                }

                manager.subtract(uuid, cost);
                PlayTimeLimiterAPI api = PlayTimeLimiter.getInstance().getAPI();
                api.reduceDailyUsed(uuid, minutes);
                player.sendMessage("§aPurchased §f" + minutes + " minute(s) §afor §b" + cost + " Timerald.");
                return true;
            }

            if (category.equalsIgnoreCase("elixir")) {
                int amount;
                try {
                    amount = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number.");
                    return true;
                }

                ConfigurationSection elixir = shop.getConfigurationSection("elixir");
                int cost = elixir.getInt("cost");

                int total = cost * amount;
                if (manager.get(uuid) < total) {
                    player.sendMessage("§cNot enough Timerald. You need §b" + total + "§c.");
                    return true;
                }

                manager.subtract(uuid, total);

                Material material = Material.getMaterial(elixir.getString("material", "POTION"));
                if (material == null) {
                    player.sendMessage("§cInvalid material in config.");
                    return true;
                }

                String name = elixir.getString("name", "§dTime Elixir");
                List<String> lore = elixir.getStringList("lore");

                for (int i = 0; i < amount; i++) {
                    ItemStack item = new ItemStack(material, 1);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(name);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    player.getInventory().addItem(item);
                }

                player.sendMessage("§aPurchased §f" + amount + " §dTime Elixir §afor §b" + total + " Timerald.");
                return true;
            }
        }

        // Default: shop view

        new TimeraldShopGUI(plugin).open(player);
        // int balance = manager.get(uuid);
        // player.sendMessage("§eYou have §b" + balance + " Timerald§e.");

        // ConfigurationSection shop = plugin.getConfig().getConfigurationSection("shop");
        // if (shop == null) {
        //     player.sendMessage("§cShop not configured.");
        //     return true;
        // }

        // player.sendMessage("§e§lTimerald Shop:");
        // ConfigurationSection playtime = shop.getConfigurationSection("playtime");
        // if (playtime != null) {
        //     for (String minutes : playtime.getKeys(false)) {
        //         int cost = playtime.getInt(minutes);
        //         TextComponent msg = new TextComponent("§7Buy §f" + minutes + " §7minutes for §b" + cost + " Timerald §8[§aClick here§8]");
        //         msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/timerald buy playtime " + minutes));
        //         player.spigot().sendMessage(msg);
        //     }
        // }

        // if (shop.contains("elixir")) {
        //     int cost = shop.getConfigurationSection("elixir").getInt("cost");
        //     TextComponent msg = new TextComponent("§7Buy §dTime Elixir §7for §b" + cost + " Timerald §8[§aClick here§8]");
        //     msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/timerald buy elixir 1"));
        //     player.spigot().sendMessage(msg);
        // }

        return true;
    }
}