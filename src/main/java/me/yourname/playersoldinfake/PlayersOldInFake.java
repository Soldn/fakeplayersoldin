package me.yourname.playersoldinfake;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayersOldInFake extends JavaPlugin {

    private static PlayersOldInFake instance;
    private FakePlayerManager manager;
    private TabService tabService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // ProtocolLib presence notice for TAB
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().warning("ProtocolLib не найден! TAB-список работать не будет. Чат/команды останутся.");
        }

        tabService = new TabService(this);
        manager = new FakePlayerManager(this, tabService);

        getCommand("playersoldinfake").setExecutor(new PluginCommandHandler(this));

        manager.startTasks();

        getLogger().info("playersoldinfake включён.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.clearFakePlayers();
        }
        if (tabService != null) {
            tabService.clearAllFromTab();
        }
        getLogger().info("playersoldinfake выключен.");
    }

    public static PlayersOldInFake getInstance() {
        return instance;
    }

    public FakePlayerManager getManager() {
        return manager;
    }

    public TabService getTabService() {
        return tabService;
    }
}
