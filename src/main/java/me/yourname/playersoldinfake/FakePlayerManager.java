package me.yourname.playersoldinfake;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FakePlayerManager {

    private final PlayersOldInFake plugin;
    private final TabService tabService;
    private final Set<String> activeFakes = new HashSet<>();
    private final Random random = new Random();

    public FakePlayerManager(PlayersOldInFake plugin, TabService tabService) {
        this.plugin = plugin;
        this.tabService = tabService;
    }

    public void startTasks() {
        int joinLeaveInterval = plugin.getConfig().getInt("settings.join-leave-interval");
        int chatInterval = plugin.getConfig().getInt("settings.chat-interval");

        // maintain TAB min players
        new BukkitRunnable() {
            @Override
            public void run() {
                maintainTabMinimum();
            }
        }.runTaskTimer(plugin, 40L, 20L * 15); // каждые 15 секунд проверка

        new BukkitRunnable() {
            @Override
            public void run() {
                simulateJoinLeave();
            }
        }.runTaskTimer(plugin, 20L, joinLeaveInterval * 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                simulateChat();
            }
        }.runTaskTimer(plugin, 60L, chatInterval * 20L);
    }

    private void maintainTabMinimum() {
        if (!plugin.getConfig().getBoolean("settings.tab-force-online", true)) return;

        int min = plugin.getConfig().getInt("settings.min-players");
        int onlineReal = Bukkit.getOnlinePlayers().size();
        int toHave = Math.max(min, 0);

        // Ensure at least 'toHave' total names appear by topping up with fakes
        List<String> pool = new ArrayList<>(plugin.getConfig().getStringList("fake-names"));
        Collections.shuffle(pool);

        while (activeFakes.size() + onlineReal < toHave && !pool.isEmpty()) {
            String name = pickFree(pool);
            if (name == null) break;
            if (activeFakes.add(name)) {
                tabService.addToTab(name);
            }
        }

        // Respect max-players cap
        int max = plugin.getConfig().getInt("settings.max-players");
        while (activeFakes.size() + onlineReal > max && !activeFakes.isEmpty()) {
            String name = getRandomActive();
            removeFake(name);
        }
    }

    private void simulateJoinLeave() {
        List<String> allNames = plugin.getConfig().getStringList("fake-names");
        if (allNames.isEmpty()) return;

        int min = plugin.getConfig().getInt("settings.min-players");
        int max = plugin.getConfig().getInt("settings.max-players");

        boolean doJoin = activeFakes.size() < min || random.nextBoolean();
        if (doJoin && activeFakes.size() < max) {
            String name = pickFree(allNames);
            if (name != null && activeFakes.add(name)) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + name + " joined the game");
                tabService.addToTab(name);
            }
        } else if (!activeFakes.isEmpty()) {
            String name = getRandomActive();
            removeFake(name);
        }
    }

    private void simulateChat() {
        if (activeFakes.isEmpty()) return;
        List<String> messages = plugin.getConfig().getStringList("chat-messages");
        if (messages.isEmpty()) return;

        String sender = getRandomActive();
        String msg = messages.get(random.nextInt(messages.size()));
        Bukkit.broadcastMessage(ChatColor.GRAY + "<" + sender + "> " + msg);
    }

    private String pickFree(List<String> names) {
        List<String> free = new ArrayList<>(names);
        free.removeAll(activeFakes);
        if (free.isEmpty()) return null;
        return free.get(random.nextInt(free.size()));
    }

    private String getRandomActive() {
        int i = random.nextInt(activeFakes.size());
        return new ArrayList<>(activeFakes).get(i);
    }

    public void addFake(String name) {
        if (activeFakes.add(name)) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + name + " joined the game");
            tabService.addToTab(name);
        }
    }

    public void removeFake(String name) {
        if (activeFakes.remove(name)) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + name + " left the game");
            tabService.removeFromTab(name);
        }
    }

    public void clearFakePlayers() {
        for (String n : new ArrayList<>(activeFakes)) {
            removeFake(n);
        }
        activeFakes.clear();
    }
}
