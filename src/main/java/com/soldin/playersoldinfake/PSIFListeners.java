package com.soldin.playersoldinfake;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PSIFListeners implements Listener {
    private final FakePlayerManager manager;

    public PSIFListeners(FakePlayerManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // When a real player joins, ensure they see all current fake entries in TAB
        manager.refreshTabFor(e.getPlayer());
    }
}
