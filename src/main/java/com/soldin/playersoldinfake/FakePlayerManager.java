package com.soldin.playersoldinfake;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class FakePlayerManager {

    private final PlayerSoldInFakePlugin plugin;
    private final ProtocolManager protocol;
    private final Map<String, FakePlayer> fakePlayers = new LinkedHashMap<>();

    private int minOnline;
    private int maxOnline;
    private int tickIntervalSeconds;
    private int sessionMinutesMin;
    private int sessionMinutesMax;
    private double chatChance;
    private boolean showJoinQuit;
    private String fmtJoin, fmtQuit, fmtChat;
    private List<String> baseMessages = new ArrayList<>();
    private List<String> baseNames = new ArrayList<>();
    private List<String> genPrefixes = new ArrayList<>();
    private List<String> genSuffixes = new ArrayList<>();

    private BukkitTask task;

    public FakePlayerManager(PlayerSoldInFakePlugin plugin) {
        this.plugin = plugin;
        this.protocol = ProtocolLibrary.getProtocolManager();
    }

    public void loadFromConfig(FileConfiguration cfg) {
        fakePlayers.clear();

        this.minOnline = Math.max(0, cfg.getInt("min-fake-online", 4));
        this.maxOnline = Math.max(this.minOnline, cfg.getInt("max-fake-online", 15));
        this.tickIntervalSeconds = Math.max(5, cfg.getInt("tick-interval-seconds", 20));
        this.sessionMinutesMin = Math.max(5, cfg.getInt("session-minutes-min", 120));
        this.sessionMinutesMax = Math.max(this.sessionMinutesMin, cfg.getInt("session-minutes-max", 180));
        this.chatChance = Math.max(0, Math.min(1, cfg.getDouble("chat-random-chance", 0.03)));
        this.showJoinQuit = cfg.getBoolean("fake-join-quit-messages", true);

        this.fmtJoin = ChatColor.translateAlternateColorCodes('&', cfg.getString("formats.join", "&e%name% &7зашёл на сервер."));
        this.fmtQuit = ChatColor.translateAlternateColorCodes('&', cfg.getString("formats.quit", "&e%name% &7вышел с сервера."));
        this.fmtChat = ChatColor.translateAlternateColorCodes('&', cfg.getString("formats.chat", "&7%name%: &f%message%"));

        this.baseMessages = cfg.getStringList("chat-messages");
        this.baseNames = cfg.getStringList("names");
        this.genPrefixes = cfg.getStringList("name-gen.prefixes");
        this.genSuffixes = cfg.getStringList("name-gen.suffixes");

        // preload some fakes from base names
        for (String n : baseNames) {
            addFakeInternal(n, false);
        }

        plugin.getLogger().info("Config loaded. Names=" + fakePlayers.size());
        // Refresh to all online
        for (Player p : Bukkit.getOnlinePlayers()) {
            refreshTabFor(p);
        }
    }

    public void startTask() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, tickIntervalSeconds * 20L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        // remove all entries from viewers
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendRemoveAllTo(p);
        }
        fakePlayers.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        int online = (int) fakePlayers.values().stream().filter(FakePlayer::isOnline).count();

        // Ensure minimum
        if (online < minOnline) {
            int need = Math.min(minOnline - online, maxOnline - online);
            bringOnline(need);
        } else {
            // Randomly end sessions if exceeded or random
            for (FakePlayer fp : fakePlayers.values()) {
                if (fp.isOnline() && fp.getSessionEndMillis() > 0 && now >= fp.getSessionEndMillis()) {
                    setOffline(fp);
                }
            }
            // Random minor fluctuations up to max
            if (online < maxOnline && ThreadLocalRandom.current().nextDouble() < 0.25) {
                bringOnline(1);
            }
            if (online > minOnline && ThreadLocalRandom.current().nextDouble() < 0.20) {
                // pick someone to leave
                List<FakePlayer> on = fakePlayers.values().stream().filter(FakePlayer::isOnline).collect(Collectors.toList());
                if (!on.isEmpty()) setOffline(on.get(ThreadLocalRandom.current().nextInt(on.size())));
            }
        }

        // Random chat
        if (ThreadLocalRandom.current().nextDouble() < chatChance) {
            List<FakePlayer> on = fakePlayers.values().stream().filter(FakePlayer::isOnline).collect(Collectors.toList());
            if (!on.isEmpty() && !baseMessages.isEmpty()) {
                FakePlayer fp = on.get(ThreadLocalRandom.current().nextInt(on.size()));
                String msg = baseMessages.get(ThreadLocalRandom.current().nextInt(baseMessages.size()));
                broadcastFormatted(fmtChat.replace("%name%", fp.getName()).replace("%message%", msg));
            }
        }
    }

    private void bringOnline(int count) {
        List<FakePlayer> offs = fakePlayers.values().stream().filter(fp -> !fp.isOnline()).collect(Collectors.toList());
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i=0; i<count; i++) {
            FakePlayer fp;
            if (!offs.isEmpty()) {
                fp = offs.remove(rnd.nextInt(offs.size()));
            } else {
                // Create a new generated name
                String gen = generateName();
                fp = addFakeInternal(gen, true);
            }
            if (fp == null) continue;
            setOnline(fp);
        }
    }

    private String generateName() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String base = baseNames.isEmpty() ? "Player" : baseNames.get(r.nextInt(baseNames.size()));
        if (!genPrefixes.isEmpty() && r.nextBoolean()) base = genPrefixes.get(r.nextInt(genPrefixes.size())) + base;
        if (!genSuffixes.isEmpty() && r.nextBoolean()) base = base + genSuffixes.get(r.nextInt(genSuffixes.size()));
        // Ensure unique
        String name = base;
        int tries = 0;
        while (fakePlayers.containsKey(name) && tries < 20) {
            name = base + r.nextInt(10, 99);
            tries++;
        }
        return name;
    }

    public void refreshTabFor(Player viewer) {
        // Remove all our fakes first, then add only online ones
        sendRemoveAllTo(viewer);
        List<FakePlayer> online = fakePlayers.values().stream().filter(FakePlayer::isOnline).collect(Collectors.toList());
        if (!online.isEmpty()) {
            sendAddListTo(viewer, online);
        }
    }

    public boolean addFake(String name) {
        return addFakeInternal(name, true) != null;
    }

    private FakePlayer addFakeInternal(String name, boolean generateUUIDIfMissing) {
        if (fakePlayers.containsKey(name)) return null;
        UUID uuid = UUID.nameUUIDFromBytes(("PSIF:" + name).getBytes());
        FakePlayer fp = new FakePlayer(uuid, name);
        fakePlayers.put(name, fp);
        return fp;
    }

    public int addRandom(int n) {
        int added = 0;
        for (int i=0; i<n; i++) {
            String name = generateName();
            if (addFake(name)) added++;
        }
        return added;
    }

    public boolean removeFake(String name) {
        FakePlayer fp = fakePlayers.remove(name);
        if (fp == null) return false;
        // Inform viewers
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendRemoveTo(p, Collections.singletonList(fp.getUuid()));
        }
        if (fp.isOnline() && showJoinQuit) {
            broadcastFormatted(fmtQuit.replace("%name%", name));
        }
        return true;
    }

    public void removeAll() {
        List<UUID> all = fakePlayers.values().stream().map(FakePlayer::getUuid).collect(Collectors.toList());
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendRemoveTo(p, all);
        }
        fakePlayers.clear();
    }

    private void setOnline(FakePlayer fp) {
        if (fp.isOnline()) return;
        fp.setOnline(true);
        long durationMin = ThreadLocalRandom.current().nextLong(sessionMinutesMin, sessionMinutesMax + 1L);
        fp.setSessionEndMillis(System.currentTimeMillis() + durationMin * 60_000L);
        // Send add to all viewers
        List<FakePlayer> list = Collections.singletonList(fp);
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendAddListTo(p, list);
        }
        if (showJoinQuit) {
            broadcastFormatted(fmtJoin.replace("%name%", fp.getName()));
        }
    }

    private void setOffline(FakePlayer fp) {
        if (!fp.isOnline()) return;
        fp.setOnline(false);
        // Send remove
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendRemoveTo(p, Collections.singletonList(fp.getUuid()));
        }
        if (showJoinQuit) {
            broadcastFormatted(fmtQuit.replace("%name%", fp.getName()));
        }
    }

    private void broadcastFormatted(String text) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(text));
        Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(text));
    }

    // ===== ProtocolLib helpers (handles 1.16..1.21 via new/old packets) =====

    private void sendAddListTo(Player viewer, List<FakePlayer> fps) {
        try {
            if (supportsNewPlayerInfoPackets()) {
                // 1.19.3+ uses PLAYER_INFO_UPDATE
                PacketContainer update = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO_UPDATE);
                // Actions
                EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(
                        EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                        EnumWrappers.PlayerInfoAction.UPDATE_LISTED,
                        EnumWrappers.PlayerInfoAction.UPDATE_LATENCY,
                        EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE,
                        EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
                );
                update.getPlayerInfoActions().write(0, actions);

                List<PlayerInfoData> data = new ArrayList<>();
                for (FakePlayer fp : fps) {
                    WrappedGameProfile profile = new WrappedGameProfile(fp.getUuid(), fp.getName());
                    PlayerInfoData pid = new PlayerInfoData(profile, 50, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(fp.getName()));
                    data.add(pid);
                }
                update.getPlayerInfoDataLists().write(1, data); // index 1 for entries in new packet
                protocol.sendServerPacket(viewer, update);
            } else {
                // <= 1.19.2
                PacketContainer add = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
                add.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                List<PlayerInfoData> data = new ArrayList<>();
                for (FakePlayer fp : fps) {
                    WrappedGameProfile profile = new WrappedGameProfile(fp.getUuid(), fp.getName());
                    data.add(new PlayerInfoData(profile, 50, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(fp.getName())));
                }
                add.getPlayerInfoDataLists().write(0, data);
                protocol.sendServerPacket(viewer, add);
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void sendRemoveTo(Player viewer, List<UUID> uuids) {
        try {
            if (supportsNewPlayerInfoPackets()) {
                // 1.19.3+ PLAYER_INFO_REMOVE
                PacketContainer rm = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
                rm.getUUIDLists().write(0, uuids);
                protocol.sendServerPacket(viewer, rm);
            } else {
                // <= 1.19.2 use PLAYER_INFO with REMOVE_PLAYER
                PacketContainer pk = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
                pk.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                List<PlayerInfoData> data = new ArrayList<>();
                for (UUID id : uuids) {
                    // Only UUID matters for removal; name can be null
                    WrappedGameProfile profile = new WrappedGameProfile(id, null);
                    data.add(new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null));
                }
                pk.getPlayerInfoDataLists().write(0, data);
                protocol.sendServerPacket(viewer, pk);
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void sendRemoveAllTo(Player viewer) {
        List<UUID> all = fakePlayers.values().stream().map(FakePlayer::getUuid).collect(Collectors.toList());
        if (!all.isEmpty()) sendRemoveTo(viewer, all);
    }

    private boolean supportsNewPlayerInfoPackets() {
        // ProtocolLib exposes PLAYER_INFO_UPDATE and PLAYER_INFO_REMOVE on modern versions only
        return PacketType.Play.Server.PLAYER_INFO_UPDATE != null && PacketType.Play.Server.PLAYER_INFO_REMOVE != null;
    }

    public List<String> getAllFakeNames() {
        return new ArrayList<>(fakePlayers.keySet());
    }
}
