package com.darkun7.timerald.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ClickChainListener implements Listener {

    private final JavaPlugin plugin;

    // How long (ms) before a combo resets
    private final long timeout = 1200;

    // Stores per-player click history
    private final Map<UUID, List<ClickEntry>> clickHistory = new HashMap<>();

    // Command mappings
    private final Map<List<ClickType>, String> comboCommands = new HashMap<>();

    public ClickChainListener(JavaPlugin plugin) {
        this.plugin = plugin;

        // Register combos
        registerCombo(List.of(ClickType.RIGHT, ClickType.RIGHT, ClickType.RIGHT), "timerald");
        registerCombo(List.of(ClickType.RIGHT, ClickType.LEFT, ClickType.RIGHT), "shop");
    }

    public enum ClickType { LEFT, RIGHT }

    private static class ClickEntry {
        ClickType type;
        long time;

        ClickEntry(ClickType type, long time) {
            this.type = type;
            this.time = time;
        }
    }

    private void registerCombo(List<ClickType> combo, String command) {
        comboCommands.put(combo, command);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!player.getInventory().getItemInMainHand().getType().isAir()){
            return;
        }

        ClickType click = null;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            click = ClickType.RIGHT;
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            click = ClickType.LEFT;
        } else {
            return; // not a click we care about
        }

        // Get and update click history
        List<ClickEntry> history = clickHistory.getOrDefault(uuid, new ArrayList<>());
        long now = System.currentTimeMillis();

        // Remove old entries
        history.removeIf(entry -> now - entry.time > timeout);

        // Add new entry
        history.add(new ClickEntry(click, now));
        clickHistory.put(uuid, history);

        // Check for matches
        List<ClickType> currentPattern = new ArrayList<>();
        for (ClickEntry entry : history) currentPattern.add(entry.type);

        for (Map.Entry<List<ClickType>, String> combo : comboCommands.entrySet()) {
            if (endsWith(currentPattern, combo.getKey())) {
                // player.sendMessage("§aCombo matched! Running /" + combo.getValue());
                player.performCommand(combo.getValue());

                // Reset history after match
                clickHistory.put(uuid, new ArrayList<>());
                break;
            }
        }
    }


    private boolean endsWith(List<ClickType> full, List<ClickType> pattern) {
        if (full.size() < pattern.size()) return false;
        int offset = full.size() - pattern.size();
        for (int i = 0; i < pattern.size(); i++) {
            if (full.get(i + offset) != pattern.get(i)) return false;
        }
        return true;
    }
}
