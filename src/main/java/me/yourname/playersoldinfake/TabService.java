package me.yourname.playersoldinfake;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class TabService {

    private final PlayersOldInFake plugin;
    private final ProtocolManager protocol;
    private final Map<String, UUID> fakeUuids = new HashMap<>();
    private final boolean hasProtocolLib;

    public TabService(PlayersOldInFake plugin) {
        this.plugin = plugin;
        this.hasProtocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
        this.protocol = hasProtocolLib ? ProtocolLibrary.getProtocolManager() : null;
    }

    public void addToTab(String name) {
        if (!hasProtocolLib) return;
        UUID id = fakeUuids.computeIfAbsent(name, k -> UUID.nameUUIDFromBytes(("psf:" + k).getBytes()));
        sendPlayerInfoAdd(name, id);
    }

    public void removeFromTab(String name) {
        if (!hasProtocolLib) return;
        UUID id = fakeUuids.get(name);
        if (id == null) return;
        sendPlayerInfoRemove(name, id);
    }

    public void clearAllFromTab() {
        if (!hasProtocolLib) return;
        for (Map.Entry<String, UUID> e : new HashMap<>(fakeUuids).entrySet()) {
            sendPlayerInfoRemove(e.getKey(), e.getValue());
        }
        fakeUuids.clear();
    }

    private void sendPlayerInfoAdd(String name, UUID id) {
        try {
            // For newer versions use PLAYER_INFO_UPDATE; try both for compatibility.
            PacketContainer packet;
            if (ProtocolLibrary.getProtocolManager().getMinecraftVersion().getMinor() >= 19) {
                // Use PLAYER_INFO_UPDATE when available
                packet = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
            } else {
                packet = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
            }

            WrappedGameProfile profile = new WrappedGameProfile(id, trim16(name));
            PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(trim16(name)));

            // Old API: actions + data list
            packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packet.getPlayerInfoDataLists().write(0, Collections.singletonList(data));

            broadcastPacket(packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось добавить в TAB: " + e.getMessage());
        }
    }

    private void sendPlayerInfoRemove(String name, UUID id) {
        try {
            PacketContainer packet = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
            WrappedGameProfile profile = new WrappedGameProfile(id, trim16(name));
            PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(trim16(name)));

            packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            packet.getPlayerInfoDataLists().write(0, Collections.singletonList(data));

            broadcastPacket(packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось удалить из TAB: " + e.getMessage());
        }
    }

    private void broadcastPacket(PacketContainer packet) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                protocol.sendServerPacket(p, packet);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private String trim16(String s) {
        if (s == null) return "Fake";
        return s.length() > 16 ? s.substring(0, 16) : s;
    }
}
