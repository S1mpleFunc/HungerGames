package ru.func.hungergames;

import com.sun.istack.internal.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.LinkedList;
import java.util.Random;

public class HungerListener implements Listener {

    private HungerGames plugin;
    public HungerListener (HungerGames hg) { plugin = hg; }

    public static LinkedList<Location> openned_chests = new LinkedList<>();
    private Random randomGenerator = new Random();

    GameStarter gameStarter = new GameStarter();
    PlayerGUI playerGUI = new PlayerGUI();

    @EventHandler
    public void onJoin (PlayerJoinEvent e)
    {
        //Выполнение первичных функций с игроком
        plugin.loadStats(e.getPlayer(), plugin);
        plugin.hashStats(e.getPlayer().getUniqueId(), plugin);
        plugin.updateScores(plugin, 0, 0, 0);
        e.getPlayer().getInventory().clear();
        if (GameStatus.WAITING.isActive()) {
            e.getPlayer().teleport(Lobby.center);
            e.getPlayer().setGameMode(GameMode.SURVIVAL);
        }
        else if (GameStatus.STARTED.isActive() || GameStatus.STARTING.isActive()) {
            //Выдача режима наблюдателя и заполнение инвентаря навигатаром
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            for (int i = 0; i < 9; i ++)
                e.getPlayer().getInventory().setItem(i, plugin.compass);
        }
        e.setJoinMessage(plugin.getConfig().getString("event.join") + "§l" + e.getPlayer().getName());
    }
    @EventHandler
    public void onLeave (PlayerQuitEvent e)
    {
        Player p = e.getPlayer();
        String name = p.getName();
        e.setQuitMessage(plugin.getConfig().getString("event.quit") + "§l" + name);
        p.setGameMode(GameMode.SURVIVAL);
        if (GameStatus.STARTED.isActive() || GameStatus.STARTING.isActive()) {
            if (gameStarter.life_players.contains(p))
                gameStarter.life_players.remove(p);
            //Сообщение выхода, если игра начинается или началась
            e.setQuitMessage("[§b!§f] §l" + name + " §fвышел из игры. В живых осталось(ся): §l" + gameStarter.life_players.size());
            closeGame();
            //Попытка обновления данных игрока, если у него убийсва != 0
            if (!gameStarter.kills.containsKey(name) || gameStarter.kills.get(name).equals(0))
                return;
            try {
                ResultSet rs = plugin.statement.executeQuery("SELECT * FROM `TEST` WHERE uuid = '" + p.getUniqueId() + "';");
                if (rs.next()) {
                    int new_kills = rs.getInt("kills") + gameStarter.kills.get(name);
                    int new_coins = rs.getInt("gold") + gameStarter.kills.get(name) * 5;
                    int new_deaths = rs.getInt("deaths") + 1;
                    plugin.statement.executeUpdate("UPDATE `TEST` SET kills ='" + new_kills + "', gold = '" + new_coins + "', deaths = '" + new_deaths + "' WHERE uuid = '" + p.getUniqueId() + "';");
                }
            } catch (SQLException ex) {}
        }
        //Удаление игрока из временного регистра
        plugin.playerStats.remove(p.getUniqueId());
    }
    @EventHandler
    public void onMove (PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (GameStatus.STARTING.isActive()) {
            //Плавное возвращение игрока на блок
            Location from = e.getFrom();
            Location to = e.getTo();
            double x = Math.floor(from.getX());
            double z = Math.floor(from.getZ());
            if (Math.floor(to.getX()) != x || Math.floor(to.getZ()) != z) {
                x += .5;
                z += .5;
                e.getPlayer().teleport(new Location(from.getWorld(), x, from.getY(), z, from.getYaw(), from.getPitch()));
            }
        }
        //Обновление компасов
        if (GameStatus.STARTED.isActive()) {
            if (!p.getInventory().contains(Material.COMPASS))
                return;
            HungerListener listener = new HungerListener(plugin);
            Player target = listener.getNearestPlayer(p);
            if (target == null)
                return;
            p.setCompassTarget(target.getLocation());
            if (target.getLocation().distance(p.getLocation()) < 101) {
                double xp = target.getLocation().distance(p.getLocation());
                p.setExp((float) (xp % 100) / 100);
            }
        }
    }
    @EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent e) {
        if (!GameStatus.STARTED.isActive()) {
            e.setCancelled(true);
            return;
        }
        if (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof DoubleChest) {
            if (openned_chests.contains(e.getInventory().getLocation()))
                return;
            if (!gameStarter.life_players.contains(e.getPlayer()))
                return;
            String name = e.getPlayer().getName();
            gameStarter.open_chest.replace(name, gameStarter.open_chest.get(name) + 1);

            int min = plugin.getConfig().getInt("range.min");
            int max = plugin.getConfig().getInt("range.max");
            //Заполнение сундука псевдослучайными вещами
            int random_size = (int) (Math.random() * ((max - min) + 1)) + min;
            if (gameStarter.open_chest.get(name) % 4 == 0) {
                //Если номер открытого игроком сундука кратен четырем, то лут хороший
                chestSetter(plugin.good_items, e.getInventory(), random_size);
                gameStarter.open_chest.replace(name, 0);
            } else {
                //По умолчанию лут - плохой
                chestSetter(plugin.bad_items, e.getInventory(), random_size);
                e.getInventory().addItem(plugin.food_items.get(randomGenerator.nextInt(plugin.food_items.size())));
            }
            //Возможное добавление личных предметов, по особым признакам
            if (e.getPlayer().getInventory().contains(Material.BOW))
                e.getInventory().addItem(new ItemStack(Material.ARROW, (int) (Math.random() * 6)));
            //Фиксирование открытия сундука
            openned_chests.add(e.getInventory().getLocation());
            e.setCancelled(false);
        }
    }
    @EventHandler
    public void onInteract (PlayerInteractEvent e)
    {
        if (GameStatus.WAITING.isActive())
            return;
        Player p = e.getPlayer();
        if (p.getItemInHand().getType().equals(Material.COMPASS))
            if (p.getGameMode().equals(GameMode.SPECTATOR))
                //Открытие меню наблюдателя
                playerGUI.openPlayerGUI(plugin, p);
    }
    @EventHandler
    public void onClick (InventoryClickEvent e)
    {
        Player p = (Player) e.getWhoClicked();
        Inventory open = e.getInventory();
        ItemStack item = e.getCurrentItem();
        if (open == null || item == null || !item.hasItemMeta())
            return;
        if (item.getType().equals(Material.STAINED_GLASS_PANE)) {
            e.setCancelled(true);
            return;
        }
        //Обработка действия по выбранному предмету
        playerGUI.teamGUIHandler(plugin, p, item);
        e.setCancelled(true);
    }
    // При убийстве
    @EventHandler
    public void onDeath (PlayerDeathEvent e) {
        Player death = e.getEntity();
        Player killer = death.getKiller();
        if (gameStarter.life_players.contains(death))
            //Удаление игрока из живых
            gameStarter.life_players.remove(death);
        try {
            //Если игрока убил другой игрок
            e.setDeathMessage("[§b!§f] §l" + killer.getName() + " §fубил §l" + death.getName() + "§f. В живых осталось(ся): §l" + gameStarter.life_players.size());
            gameStarter.kills.replace(killer.getName(), gameStarter.kills.get(killer.getName()) + 1);
            killer.setFoodLevel(20);
        } catch (NullPointerException ex)
        {
            //Если игрок умер сам
            e.setDeathMessage("[§b!§f] §l" + death.getName() + " §fумер. В живых осталось(ся): §l" + gameStarter.life_players.size());
        }
        plugin.updateScores(plugin, 0, 0, 0);
        closeGame();
    }
    private void chestSetter (@NotNull LinkedList<ItemStack> items, @NotNull Inventory inv, int size)
    {
        inv.clear();
        //Заполняет сундук случайными НЕ повторяющимищя вещами, из LinkedList<ItemStack> items списка
        LinkedList<ItemStack> no_copy = new LinkedList<>();
        for (int v = 0; v < size; v++) {
            ItemStack item = items.get(randomGenerator.nextInt(size));
            no_copy.add(item);
            items.remove(item);
            inv.setItem(randomGenerator.nextInt(27), item);
        }
        items.addAll(no_copy);
        no_copy.clear();
    }
    private void closeGame ()
    {
        if (gameStarter.life_players.size() != 1)
            return;
        //Завершение игры
        //Очистка открытых сундуков
        for (Location loc : openned_chests) {
            Chest chest = (Chest) loc.getBlock().getState();
            chest.getBlockInventory().clear();
        }
        Player winner = gameStarter.life_players.get(0);
        GameStatus.FINISHING.setActive();
        saveStats();
        Bukkit.broadcastMessage(plugin.getConfig().getString("game.kills_message"));
        //Выведение списка убийств за игру
        Bukkit.getOnlinePlayers().stream().filter(p -> gameStarter.kills.containsKey(p.getName()) && gameStarter.kills.get(p.getName()) != 0).forEach(p -> Bukkit.broadcastMessage("  *  " + p.getName() + " §fубил §c§l" + gameStarter.kills.get(p.getName()) + "§f игроков(а)."));
        //Выведение имени победителя
        plugin.sendTitle("[§a!§f]", "Победа!");
        Bukkit.broadcastMessage("[§a!§f]§l " + winner.getName() + " §f победил!");
        //Запуск таймера для перезагрузки
        new BukkitRunnable() {
            int end_time = plugin.getConfig().getInt("end_time");
            @Override
            public void run() {
                if (end_time == 0)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                Bukkit.broadcastMessage(plugin.getConfig().getString("game.end_message") + end_time);
                end_time = end_time - 1;
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    private void saveStats () {
        //Сохраняет статистику для всех игроков
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                String name = p.getName();
                ResultSet rs = plugin.statement.executeQuery("SELECT * FROM `TEST` WHERE uuid = '" + p.getUniqueId() + "';");
                if (rs.next()) {
                    int new_kills = rs.getInt("kills") + gameStarter.kills.get(name);
                    int new_coins = rs.getInt("gold") + gameStarter.kills.get(name) * 5;
                    int new_deaths = rs.getInt("deaths") + 1;
                    if (gameStarter.life_players.contains(p)) {
                        int new_wins = rs.getInt("wins") + 1;
                        plugin.statement.executeUpdate("UPDATE `TEST` SET kills ='" + new_kills + "', gold = '" + new_coins + "', wins = '" + new_wins + "' WHERE uuid = '" + p.getUniqueId() + "';");
                    }
                    else
                        plugin.statement.executeUpdate("UPDATE `TEST` SET kills ='" + new_kills + "', gold = '" + new_coins + "', deaths = '" + new_deaths + "' WHERE uuid = '" + p.getUniqueId() + "';");
                }
            } catch (SQLException | NullPointerException ex) {
            }
        }
    }
    public Player getNearestPlayer (Player player) {
        double distNear = 0.0D;
        Player playerNear = null;
        for (Player player2 : Bukkit.getOnlinePlayers()) {
            if (player == player2)
                continue;
            if (player.getWorld() != player2.getWorld())
                continue;
            if (!GameStarter.life_players.contains(player2))
                continue;
            Location location2 = player.getLocation();
            double dist = player2.getLocation().distance(location2);
            if (playerNear == null || dist < distNear) {
                playerNear = player2;
                distNear = dist;
            }
        }
        return playerNear;
    }
    @EventHandler
    public void onRespawn (PlayerRespawnEvent e)
    {
        if (!GameStatus.WAITING.isActive()) {
            //Выдача режима наблюдателя и навигатора
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            for (int i = 0; i < 9; i ++)
                e.getPlayer().getInventory().setItem(i, plugin.compass);
        }
    }
    @EventHandler
    public void onBlockBreak (BlockBreakEvent e)
    {
        //Ломать можно только листву (и если игра началась), или если ломает OP
        if (e.getBlock().getType().equals(Material.LEAVES) && GameStatus.STARTED.isActive())
            return;
        if (e.getPlayer().isOp())
            return;
        e.setCancelled(true);
        e.getBlock().getDrops().clear();
    }
    //В лобби
    @EventHandler
    public void onDamage (EntityDamageEvent e)
    {
        if (GameStatus.WAITING.isActive() || GameStatus.STARTING.isActive())
            e.setCancelled(true);
    }
    @EventHandler
    public void onSaturation (FoodLevelChangeEvent e)
    {
        if (GameStatus.WAITING.isActive() || GameStatus.STARTING.isActive())
            e.setCancelled(true);
    }
}