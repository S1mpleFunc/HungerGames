package ru.func.hungergames;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.LinkedList;

public class GameStarter {

    public static HashMap<String, Integer> kills = new HashMap<>();
    public static HashMap<String, Integer> open_chest = new HashMap<>();
    public static LinkedList<Player> life_players = new LinkedList<>();

    public static void startGame (HungerGames plugin) {
        int online = Bukkit.getOnlinePlayers().size();
        int people_need = plugin.getConfig().getInt("people_need");
        if (online < people_need) {
            Bukkit.broadcastMessage("[§c!§f] Недостаточно игроков до начала, нужно еще §l" + (people_need - online));
            HungerGames.sendTitle("[§c§l!§f]", "Игра Отменена");
            Lobby.waitLobby(plugin);
            return;
        }
        GameStatus.STARTING.setActive();
        for (Player p : Bukkit.getOnlinePlayers()) {
            kills.put(p.getName(), 0);
            life_players.add(p);
            open_chest.put(p.getName(), 0);
            p.getInventory().clear();
            p.setFoodLevel(20);
        }
        setLocation(plugin);
        HungerGames.updateScores(plugin, 0, 0, 0);
        new BukkitRunnable() {
            int time_for_starting = plugin.getConfig().getInt("game.starting_time");
            int time_for_chest = plugin.getConfig().getInt("chest.reload");
            int value_of_chest_replace = plugin.getConfig().getInt("chest.reload_value");
            int death_time = plugin.getConfig().getInt("death_match");
            int death_size = plugin.getConfig().getInt("game.size");
            @Override
            public void run() {
                if (GameStatus.FINISHING.isActive())
                    this.cancel();
                if (time_for_starting == 0) {
                    Bukkit.broadcastMessage(plugin.getConfig().getString("game.start_message"));
                    HungerGames.sendTitle("[§a§l!§f]", "Игра Началась");
                    GameStatus.STARTED.setActive();
                }
                else if (GameStatus.STARTING.isActive())
                    Bukkit.broadcastMessage(plugin.getConfig().getString("game.starting_message") + time_for_starting + "!");
                HungerGames.updateScores(plugin, time_for_starting, 0, 0);
                time_for_starting = time_for_starting - 1;
                // ИГРА НАЧАЛАСЬ
                if (!GameStatus.STARTED.isActive())
                    return;
                if (value_of_chest_replace == 0) {
                    death_time = death_time - 1;
                    if (death_time == 0) {
                        setLocation(plugin);
                        HungerGames.sendTitle("[§c§l!§f]", "Последний бой");
                        Bukkit.broadcastMessage(plugin.getConfig().getString("game.fight_message"));
                        Bukkit.getWorld(plugin.getConfig().getString("lobby.world")).getWorldBorder().setCenter(Lobby.center);
                        Bukkit.getWorld(plugin.getConfig().getString("lobby.world")).getWorldBorder().setSize(death_size);
                    } else if (death_time < 0) {
                        Bukkit.getWorld(plugin.getConfig().getString("lobby.world")).getWorldBorder().setSize(Bukkit.getWorld(plugin.getConfig().getString("lobby.world")).getWorldBorder().getSize() - 1);
                        if (Bukkit.getWorld(plugin.getConfig().getString("lobby.world")).getWorldBorder().getSize() == 1)
                            this.cancel();
                    }
                    else
                        HungerGames.updateScores(plugin, 0, 0, death_time);
                    return;
                }
                HungerGames.updateScores(plugin, 0, time_for_chest, 0);
                if (time_for_chest == 0) {
                    time_for_chest = plugin.getConfig().getInt("chest.reload");
                    for (Location loc : HungerListener.openned_chests) {
                        Chest chest = (Chest) loc.getBlock().getState();
                        chest.getBlockInventory().clear();
                    }
                    HungerGames.sendTitle("[§a§l!§f]", "Сундуки заполнены");
                    HungerListener.openned_chests.clear();
                    Bukkit.broadcastMessage(plugin.getConfig().getString("game.chest_message"));
                    value_of_chest_replace = value_of_chest_replace - 1;
                }
                else
                    time_for_chest = time_for_chest - 1;
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    private static void setLocation (HungerGames plugin)
    {
        //Распределение игроков на окружности, с радиусом game.radius, и Окр(center)
        Location center = new Location(Bukkit.getWorld(plugin.getConfig().getString("game.world")), plugin.getConfig().getInt("game.x"), plugin.getConfig().getInt("game.y"), plugin.getConfig().getInt("game.z"));
        int radius = plugin.getConfig().getInt("game.radius");
        double x, z, angle = 0;
        //Вычсление угла. 360 / online
        for (Player p : GameStarter.life_players) {
            p.setExp(0);
            angle += 2 * Math.PI / GameStarter.life_players.size();
            x = Math.cos(angle) * radius;
            z = Math.sin(angle) * radius;
            Location point = new Location(Bukkit.getWorld(plugin.getConfig().getString("game.world")), center.getX() + x, center.getY(), center.getZ() + z);
            p.teleport(point.subtract(0.5, 0, 0.5));
        }
    }
}