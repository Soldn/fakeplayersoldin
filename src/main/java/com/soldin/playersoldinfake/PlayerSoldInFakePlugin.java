package com.soldin.playersoldinfake;

import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerSoldInFakePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Загружаем конфиг
        saveDefaultConfig();
        getLogger().info("PlayerSoldInFake включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerSoldInFake выключен!");
    }
}
