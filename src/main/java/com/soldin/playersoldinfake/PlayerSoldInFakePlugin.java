package com.soldin.playersoldinfake;

import org.bukkit.plugin.java.JavaPlugin;

public class PlayerSoldInFakePlugin extends JavaPlugin {

    private static PlayerSoldInFakePlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // создаём config.yml если его нет
        getLogger().info("PlayerSoldInFake включён! Автор: Soldi_n");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerSoldInFake выключен!");
    }

    public static PlayerSoldInFakePlugin getInstance() {
        return instance;
    }
}
