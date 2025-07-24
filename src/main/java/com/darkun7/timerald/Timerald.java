package com.darkun7.timerald;

import org.bukkit.plugin.java.JavaPlugin;
import com.darkun7.timerald.data.TimeraldManager;
import com.darkun7.timerald.listener.*;
import com.darkun7.timerald.command.DepositCommand;
import com.darkun7.timerald.command.WithdrawCommand;
import com.darkun7.timerald.command.TimeraldCommand;
import com.darkun7.timerald.command.TimeraldTabCompleter;
import com.darkun7.timerald.gui.TimeraldShopGUI;
import com.darkun7.timerald.command.ShopCommand;
import com.darkun7.timerald.command.ShopTabCompleter;

import com.darkun7.timerald.shop.ShopManager;

import java.io.File; 

public final class Timerald extends JavaPlugin {

    private static Timerald instance;
    private TimeraldManager timeraldManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        String currentVersion = getConfig().getString("config-version", "0");
        String expectedVersion = "240725-test3";
        if (currentVersion != expectedVersion) {
            getLogger().warning("Outdated config.yml detected! Regenerating with default values...");

            File configFile = new File(getDataFolder(), "config.yml");
            if (configFile.exists()) {
                configFile.delete();
            }

            saveResource("config.yml", false);
            reloadConfig();
        }

        this.timeraldManager = new TimeraldManager(this);
        this.shopManager = new ShopManager(this, this.timeraldManager);
        this.shopManager.loadShops();

        getServer().getPluginManager().registerEvents(new TimeraldListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomItemListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemUseListener(this), this);
        getServer().getPluginManager().registerEvents(new TickItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PreventPlacementListener(this), this);

        getCommand("deposit").setExecutor(new DepositCommand(this));
        getCommand("withdraw").setExecutor(new WithdrawCommand(this));
        getCommand("timerald").setExecutor(new TimeraldCommand(this));
        getCommand("timerald").setTabCompleter(new TimeraldTabCompleter());
        getCommand("shop").setExecutor(new ShopCommand(this, this.shopManager));
        getCommand("shop").setTabCompleter(new ShopTabCompleter(this.shopManager));
        
        new TimeraldShopGUI(this);

        getLogger().info("Timerald plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (this.shopManager != null) {
            this.shopManager.saveShops();
        }
    }

    public static Timerald getInstance() {
        return instance;
    }

    public TimeraldManager getTimeraldManager() {
        return timeraldManager;
    }

}
