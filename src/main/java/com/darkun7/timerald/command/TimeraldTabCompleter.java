package com.darkun7.timerald.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimeraldTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof Player)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("send");
            completions.add("top");
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("send")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("send")) {
            completions.add("1");
            completions.add("5");
            completions.add("10");
            completions.add("64");
            return completions;
        }

        return Collections.emptyList();
    }
}
