package com.soldin.playersoldinfake;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PSIFCommand implements CommandExecutor, TabCompleter {

    private final PlayerSoldInFakePlugin plugin;
    private final FakePlayerManager manager;

    public PSIFCommand(PlayerSoldInFakePlugin plugin, FakePlayerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playersoldinfake.admin")) {
            sender.sendMessage(ChatColor.RED + "Нет прав.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Использование: /psif <reload|add|addrandom|remove|removeall>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadAll();
                sender.sendMessage(ChatColor.GREEN + "Конфиг перезагружен.");
                return true;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Укажите ник: /psif add <ник>");
                    return true;
                }
                boolean added = manager.addFake(args[1]);
                sender.sendMessage(added ? ChatColor.GREEN + "Добавлен фейк: " + args[1]
                        : ChatColor.RED + "Ник уже существует.");
                return true;
            case "addrandom":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Укажите количество: /psif addrandom <n>");
                    return true;
                }
                int n;
                try { n = Integer.parseInt(args[1]); } catch (Exception ex) { sender.sendMessage(ChatColor.RED + "Число!"); return true; }
                int c = manager.addRandom(n);
                sender.sendMessage(ChatColor.GREEN + "Добавлено случайных фейков: " + c);
                return true;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Укажите ник: /psif remove <ник>");
                    return true;
                }
                boolean removed = manager.removeFake(args[1]);
                sender.sendMessage(removed ? ChatColor.GREEN + "Удалён: " + args[1]
                        : ChatColor.RED + "Ник не найден.");
                return true;
            case "removeall":
                manager.removeAll();
                sender.sendMessage(ChatColor.GREEN + "Все фейки удалены.");
                return true;
            default:
                sender.sendMessage(ChatColor.YELLOW + "Неизвестная подкоманда.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("playersoldinfake.admin")) return new ArrayList<>();
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("reload","add","addrandom","remove","removeall"), new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return StringUtil.copyPartialMatches(args[1], manager.getAllFakeNames(), new ArrayList<>());
        }
        return new ArrayList<>();
    }
}
