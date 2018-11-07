package ru.func.hungergames;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Lobby {

    public static Location center;

    public void waitLobby (HungerGames plugin) {
        //Запуск лобби
        GameStatus.WAITING.setActive();
        //Определение центра лобби
        center = new Location(
                Bukkit.getServer().getWorld(plugin.getConfig().getString("lobby.world")),
                plugin.getConfig().getInt("lobby.x") + 0.5F,
                plugin.getConfig().getInt("lobby.y") + 0F,
                plugin.getConfig().getInt("lobby.z") + 0.5F
        );
        //Очистка инвентарей всех игроков, выдача режима выживания всем игрокам, телепортация всех к центру
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
            p.teleport(center);
            p.setFoodLevel(20);
        }
        //Работа с миром
        Bukkit.getWorld(plugin.getConfig().getString("lobby.world")).getWorldBorder().setCenter(center);
        Bukkit.getWorld(plugin.getConfig().getString("lobby.world")).getWorldBorder().setSize(plugin.getConfig().getInt("game.default_size"));
        //Запуск таймера до начала игры
        new BukkitRunnable() {
            public int waitingTime = plugin.getConfig().getInt("waiting_time");
            public int people_need = plugin.getConfig().getInt("people_need");
            @Override
            public void run ()
            {
                waitingTime = waitingTime - 1;
                plugin.updateScores(plugin, waitingTime, 0, 0);
                //Выдает уровень
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (Bukkit.getOnlinePlayers().size() > people_need)
                        p.setExp(1);
                    else
                        p.setExp(((float) Bukkit.getOnlinePlayers().size() / people_need) % 100);
                    p.setLevel(waitingTime);
                }
                if (waitingTime == 0)
                {
                    new GameStarter().startGame(plugin);
                    this.cancel();
                }
                else if (waitingTime <= plugin.getConfig().getInt("ready.time"))
                    Bukkit.broadcastMessage(plugin.getConfig().getString("ready.message") + waitingTime + ".");
            }
        }.runTaskTimer(plugin, 0, 20);
    }
}