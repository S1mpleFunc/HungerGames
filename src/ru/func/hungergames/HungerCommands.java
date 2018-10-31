package ru.func.hungergames;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HungerCommands implements CommandExecutor {

    private HungerGames plugin;

    public HungerCommands(HungerGames hg) {
        plugin = hg;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player))
            return false;
        if (!commandSender.isOp())
            return false;
        if (strings.length < 1)
            return false;
        try {
            if (Integer.parseInt(strings[0]) <= 0 || Integer.parseInt(strings[0]) > 100) {
                commandSender.sendMessage("Некорректное число!");
                return true;
            } else {
                //Установление центра ИГРОВОГО мира
                Location location = ((Player) commandSender).getLocation();
                plugin.getConfig().set("game.radius", Integer.parseInt(strings[0]));
                plugin.getConfig().set("game.x", (int) location.getX());
                plugin.getConfig().set("game.y", (int) location.getY());
                plugin.getConfig().set("game.z", (int) location.getZ());
                Bukkit.broadcastMessage("Центр был усталовлен.");
                plugin.saveConfig();
                plugin.reloadConfig();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}