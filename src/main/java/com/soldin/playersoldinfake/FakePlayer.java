package com.soldin.playersoldinfake;

import java.util.UUID;

public class FakePlayer {
    private final UUID uuid;
    private final String name;

    // State
    private boolean online;
    private long sessionEndMillis; // when to "leave"

    public FakePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public long getSessionEndMillis() { return sessionEndMillis; }
    public void setSessionEndMillis(long sessionEndMillis) { this.sessionEndMillis = sessionEndMillis; }
}
