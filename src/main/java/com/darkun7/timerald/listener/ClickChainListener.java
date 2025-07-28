package com.darkun7.timerald.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClickChainListener implements Listener {

    private final JavaPlugin plugin;
    private final long timeout = 1200;

    private final Map<UUID, List<ClickEntry>> clickHistory = new HashMap<>();
    private final Map<List<ClickType>, String> comboCommands = new HashMap<>();

    private final File playerSettingsFile;
    private final FileConfiguration playerSettings;

    public ClickChainListener(JavaPlugin plugin) {
        this.plugin = plugin;

        // Setup YAML config for player settings
        playerSettingsFile = new File(plugin.getDataFolder(), "player_settings.yml");
        if (!playerSettingsFile.exists()) {
            try {
                playerSettingsFile.getParentFile().mkdirs();
                playerSettingsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerSettings = YamlConfiguration.loadConfiguration(playerSettingsFile);

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

        // Check if combo is enabled for this player
        if (!isComboEnabled(uuid)) return;

        // Only allow clicks with empty hand
        if (!player.getInventory().getItemInMainHand().getType().isAir()) return;

        ClickType click = null;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            click = ClickType.RIGHT;
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            click = ClickType.LEFT;
        } else {
            return;
        }

        List<ClickEntry> history = clickHistory.getOrDefault(uuid, new ArrayList<>());
        long now = System.currentTimeMillis();
        history.removeIf(entry -> now - entry.time > timeout);
        history.add(new ClickEntry(click, now));
        clickHistory.put(uuid, history);

        List<ClickType> currentPattern = new ArrayList<>();
        for (ClickEntry entry : history) currentPattern.add(entry.type);

        for (Map.Entry<List<ClickType>, String> combo : comboCommands.entrySet()) {
            if (endsWith(currentPattern, combo.getKey())) {
                player.performCommand(combo.getValue());
                clickHistory.put(uuid, new ArrayList<>()); // reset history
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

    // === Player Setting ===

    public boolean toggleCombo(UUID uuid) {
        boolean nowEnabled = !isComboEnabled(uuid);
        setComboEnabled(uuid, nowEnabled);
        return nowEnabled;
    }

    public boolean isComboEnabled(UUID uuid) {
        return playerSettings.getBoolean(uuid.toString() + ".combo-enabled", true); // default true
    }

    public void setComboEnabled(UUID uuid, boolean enabled) {
        playerSettings.set(uuid.toString() + ".combo-enabled", enabled);
        try {
            playerSettings.save(playerSettingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
