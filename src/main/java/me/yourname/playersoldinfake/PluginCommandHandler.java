package me.yourname.playersoldinfake;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Random;

public class PluginCommandHandler implements CommandExecutor {

    private final PlayersOldInFake plugin;
    private final Random rnd = new Random();

    public PluginCommandHandler(PlayersOldInFake plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/psf reload | add <ник> | remove <ник> | removeall | addrandom <кол-во>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§aКонфиг перезагружен.");
                break;
            case "add":
                if (args.length < 2) { sender.sendMessage("§c/psf add <ник>"); break; }
                plugin.getManager().addFake(args[1]);
                sender.sendMessage("§aДобавлен: §f" + args[1]);
                break;
            case "remove":
                if (args.length < 2) { sender.sendMessage("§c/psf remove <ник>"); break; }
                plugin.getManager().removeFake(args[1]);
                sender.sendMessage("§cУдалён: §f" + args[1]);
                break;
            case "removeall":
                plugin.getManager().clearFakePlayers();
                sender.sendMessage("§cВсе фейки удалены.");
                break;
            case "addrandom":
                if (args.length < 2) { sender.sendMessage("§c/psf addrandom <кол-во>"); break; }
                int count;
                try { count = Integer.parseInt(args[1]); } catch (Exception e) { sender.sendMessage("§cЧисло!"); break; }
                int len = plugin.getConfig().getInt("settings.random-nick-length", 8);
                for (int i = 0; i < count; i++) {
                    plugin.getManager().addFake(randomName(len));
                }
                sender.sendMessage("§aДобавлено случайных: §f" + count);
                break;
            default:
                sender.sendMessage("§e/psf reload | add <ник> | remove <ник> | removeall | addrandom <кол-во>");
        }
        return true;
    }

    private String randomName(int length) {
        final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString().substring(0, Math.min(16, length));
    }
}
