package me.yourname.playersoldinfake;

import org.bukkit.plugin.java.JavaPlugin;

public class PlayerSoldinFake extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("PlayersOldinFake enabled!");
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayersOldinFake disabled!");
    }
}
