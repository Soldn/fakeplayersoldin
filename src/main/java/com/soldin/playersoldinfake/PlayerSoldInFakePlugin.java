package com.soldin.playersoldinfake;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerSoldInFakePlugin extends JavaPlugin {

    private static PlayerSoldInFakePlugin instance;
    private FakePlayerManager manager;

    public static PlayerSoldInFakePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();

        // Init manager
        manager = new FakePlayerManager(this);
        manager.loadFromConfig(cfg);
        manager.startTask();

        // Register command
        PluginCommand cmd = getCommand("psif");
        if (cmd != null) {
            PSIFCommand executor = new PSIFCommand(this, manager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // Re-send tab entries to players who (re)join
        Bukkit.getPluginManager().registerEvents(new PSIFListeners(manager), this);

        getLogger().info("PlayerSoldInFake enabled.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    public void reloadAll() {
        reloadConfig();
        manager.loadFromConfig(getConfig());
    }
}
