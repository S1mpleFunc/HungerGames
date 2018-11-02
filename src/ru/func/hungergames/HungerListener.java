package ru.func.hungergames;

import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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

    @EventHandler
    public void onJoin (PlayerJoinEvent e)
    {
        //Выполнение первичных функций с игроком
        HungerGames.loadStats(e.getPlayer(), plugin);
        HungerGames.hashStats(e.getPlayer().getUniqueId());
        HungerGames.updateScores(plugin, 0, 0, 0);

        if (GameStatus.WAITING.isActive())
            e.getPlayer().teleport(Lobby.center);
        else if (GameStatus.STARTED.isActive() || GameStatus.STARTING.isActive()) {
            //Выдача режима наблюдателя и заполнение инвентаря навигатаром
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            for (int i = 0; i < 9; i ++)
                e.getPlayer().getInventory().setItem(i, HungerGames.compass);
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
            if (GameStarter.life_players.contains(p))
                GameStarter.life_players.remove(p);
            //Сообщение выхода, если игра начинается или началась
            e.setQuitMessage("[§b!§f] §l" + name + " §fвышел из игры. В живых осталось(ся): §l" + GameStarter.life_players.size());
            closeGame();
            //Попытка обновления данных игрока, если у него убийсва != 0
            if (!GameStarter.kills.containsKey(name) || GameStarter.kills.get(name).equals(0))
                return;
            try {
                ResultSet rs = HungerGames.statement.executeQuery("SELECT * FROM `TEST` WHERE uuid = '" + p.getUniqueId() + "';");
                if (rs.next()) {
                    int new_kills = rs.getInt("kills") + GameStarter.kills.get(name);
                    int new_coins = rs.getInt("gold") + GameStarter.kills.get(name) * 5;
                    int new_deaths = rs.getInt("deaths") + 1;
                    HungerGames.statement.executeUpdate("UPDATE `TEST` SET kills ='" + new_kills + "', gold = '" + new_coins + "', deaths = '" + new_deaths + "' WHERE uuid = '" + p.getUniqueId() + "';");
                }
            } catch (SQLException ex) {}
        }
        //Удаление игрока из временного регистра
        HungerGames.playerStats.remove(p.getUniqueId());
    }
    @EventHandler
    public void onMove (PlayerMoveEvent e)
    {
        if (!GameStatus.STARTING.isActive())
            return;
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
    @EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent e) {
        if (!GameStatus.STARTED.isActive()) {
            e.setCancelled(true);
            return;
        }
        if (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof DoubleChest) {
            if (openned_chests.contains(e.getInventory().getLocation()))
                return;
            if (!GameStarter.life_players.contains(e.getPlayer()))
                return;
            String name = e.getPlayer().getName();
            GameStarter.open_chest.replace(name, GameStarter.open_chest.get(name) + 1);

            int min = plugin.getConfig().getInt("range.min");
            int max = plugin.getConfig().getInt("range.max");
            //Заполнение сундука псевдослучайными вещами
            int random_size = (int) (Math.random() * ((max - min) + 1)) + min;
            if (GameStarter.open_chest.get(name) % 4 == 0) {
                //Если номер открытого игроком сундука кратен четырем, то лут хороший
                chestSetter(HungerGames.good_items, e.getInventory(), random_size);
                GameStarter.open_chest.replace(name, 0);
            } else {
                //По умолчанию лут - плохой
                chestSetter(HungerGames.bad_items, e.getInventory(), random_size);
                e.getInventory().addItem(HungerGames.food_items.get(randomGenerator.nextInt(HungerGames.food_items.size())));
            }
            //Возможное добавление личных предметов, по особым признакам
            if (e.getPlayer().getHealth() < 7)
                e.getInventory().addItem(new ItemStack(Material.POTION, 1, (short) 16389));
            else if (e.getPlayer().getInventory().contains(Material.BOW))
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
        if (e.getPlayer().getItemInHand().getType().equals(Material.COMPASS))
            //Открытие меню наблюдателя
            PlayerGUI.openPlayerGUI(e.getPlayer());
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
        PlayerGUI.teamGUIHandler(p, item);
        e.setCancelled(true);
    }
    // При убийстве
    @EventHandler
    public void onDeath (PlayerDeathEvent e) {
        Player death = e.getEntity();
        Player killer = death.getKiller();
        if (GameStarter.life_players.contains(death))
            //Удаление игрока из живых
            GameStarter.life_players.remove(death);
        try {
            //Если игрока убил другой игрок
            e.setDeathMessage("[§b!§f] §l" + killer.getName() + " §fубил §l" + death.getName() + "§f. В живых осталось(ся): §l" + GameStarter.life_players.size());
            GameStarter.kills.replace(killer.getName(), GameStarter.kills.get(killer.getName()) + 1);
            killer.setFoodLevel(20);
        } catch (NullPointerException ex)
        {
            //Если игрок умер сам
            e.setDeathMessage("[§b!§f] §l" + death.getName() + " §fумер. В живых осталось(ся): §l" + GameStarter.life_players.size());
        }
        HungerGames.updateScores(plugin, 0, 0, 0);
        closeGame();
    }
    private void chestSetter (@NotNull LinkedList<ItemStack> items, @NotNull Inventory inv, int size)
    {
        //Заполняет сундук случайными НЕ повторяющимищя вещами, из LinkedList<ItemStack> items списка
        LinkedList<ItemStack> no_copy = new LinkedList<>();
        for (int v = 0; v < size; v++) {
            ItemStack item = items.get(randomGenerator.nextInt(size));
            no_copy.add(item);
            items.remove(item);
            inv.setItem(v, item);
        }
        items.addAll(no_copy);
        no_copy.clear();
    }
    private void closeGame ()
    {
        if (GameStarter.life_players.size() != 1)
            return;
        //Завершение игры
        //Очистка открытых сундуков
        HungerListener.openned_chests.stream().forEach(loc -> {
            Chest chest = (Chest) loc.getBlock().getState();
            chest.getBlockInventory().clear();
        });
        Player winner = GameStarter.life_players.get(0);
        GameStatus.FINISHING.setActive();
        saveStats();
        Bukkit.broadcastMessage(plugin.getConfig().getString("game.kills_message"));
        //Выведение списка убийств за игру
        Bukkit.getOnlinePlayers().stream().filter(p -> GameStarter.kills.containsKey(p.getName())).forEach(p -> Bukkit.broadcastMessage("  *  " + p.getName() + " §fубил §c§l" + GameStarter.kills.get(p.getName()) + "§f игроков(а)."));
        //Выведение имени победителя
        HungerGames.sendTitle("[§a!§f]", "Победа!");
        Bukkit.broadcastMessage("[§a!§f]§l " + winner.getName() + " §f победил!");
        //Запуск таймера для перезагрузки
        new BukkitRunnable() {
            int end_time = plugin.getConfig().getInt("end_time");
            @Override
            public void run() {
                if (end_time == 0)
                    Bukkit.reload();
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
                ResultSet rs = HungerGames.statement.executeQuery("SELECT * FROM `TEST` WHERE uuid = '" + p.getUniqueId() + "';");
                if (rs.next()) {
                    int new_kills = rs.getInt("kills") + GameStarter.kills.get(name);
                    int new_coins = rs.getInt("gold") + GameStarter.kills.get(name) * 5;
                    int new_deaths = rs.getInt("deaths") + 1;
                    if (GameStarter.life_players.contains(p)) {
                        int new_wins = rs.getInt("wins") + 1;
                        HungerGames.statement.executeUpdate("UPDATE `TEST` SET kills ='" + new_kills + "', gold = '" + new_coins + "', wins = '" + new_wins + "' WHERE uuid = '" + p.getUniqueId() + "';");
                    }
                    else
                        HungerGames.statement.executeUpdate("UPDATE `TEST` SET kills ='" + new_kills + "', gold = '" + new_coins + "', deaths = '" + new_deaths + "' WHERE uuid = '" + p.getUniqueId() + "';");
                }
            } catch (SQLException | NullPointerException ex) {
            }
        }
    }
    @EventHandler
    public void onRespawn (PlayerRespawnEvent e)
    {
        if (!GameStatus.WAITING.isActive()) {
            //Выдача режима наблюдателя и навигатора
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            for (int i = 0; i < 9; i ++)
                e.getPlayer().getInventory().setItem(i, HungerGames.compass);
        }
    }
    // ОТМЕНЕННЫЕ
    @EventHandler
    public void onBlockBreak (BlockBreakEvent e)
    {
        //Ломать можно только листву (и если игра началась), или если ломает OP
        if (e.getBlock().getType().equals(Material.LEAVES) && GameStatus.STARTED.isActive())
            return;
        if (e.getPlayer().isOp())
            return;
        e.setCancelled(true);
    }
    @EventHandler
    public void onBlockPlace (BlockPlaceEvent e) {
        if (e.getPlayer().isOp())
            return;
        e.setCancelled(true);
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