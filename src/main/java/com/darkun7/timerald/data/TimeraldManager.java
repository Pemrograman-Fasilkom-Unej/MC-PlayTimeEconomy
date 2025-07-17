package com.darkun7.timerald.data;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import com.darkun7.timerald.Timerald;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class TimeraldManager {
    private final Timerald plugin;
    private final File dataFile;
    private final YamlConfiguration config;

    private final HashMap<UUID, Integer> balances = new HashMap<>();
    private final HashMap<UUID, UUID> aliasMap = new HashMap<>(); // Bedrock UUID -> Java UUID

    public TimeraldManager(Timerald plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    // Register alias for Bedrock UUID -> Java UUID
    public void registerAlias(UUID realUUID, String playerName) {
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(playerName)) {
                UUID javaUUID = offline.getUniqueId();
                if (!javaUUID.equals(realUUID)) {
                    aliasMap.put(realUUID, javaUUID);
                    plugin.getLogger().info("Mapped Bedrock UUID " + realUUID + " to Java UUID " + javaUUID + " for player " + playerName);
                    return;
                }
            }
        }
    }

    // Resolves UUID alias (Bedrock -> Java)
    private UUID resolve(UUID uuid) {
        return aliasMap.getOrDefault(uuid, uuid);
    }

    public int get(UUID uuid) {
        return balances.getOrDefault(resolve(uuid), 0);
    }

    public void set(UUID uuid, int value) {
        UUID resolved = resolve(uuid);
        balances.put(resolved, value);
        config.set(resolved.toString(), value);
        save();
    }

    public void add(UUID uuid, int value) {
        set(uuid, get(uuid) + value);
    }

    public boolean subtract(UUID uuid, int value) {
        int current = get(uuid);
        if (current < value) return false;
        set(uuid, current - value);
        return true;
    }

    public void save() {
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save Timerald data!");
            e.printStackTrace();
        }
    }

    public void load() {
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                balances.put(uuid, config.getInt(key));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in Timerald data file: " + key);
            }
        }
    }
}
