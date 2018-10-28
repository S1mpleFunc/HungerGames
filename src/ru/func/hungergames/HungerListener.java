package ru.func.hungergames;

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

    HungerGames plugin = HungerGames.getInstance();

    public static LinkedList<Location> openned_chests = new LinkedList<>();
    private Random randomGenerator = new Random();

    @EventHandler
    public void onJoin (PlayerJoinEvent e)
    {
        if (GameStatus.WAITING.isActive()) {
            HungerGames.loadStats(e.getPlayer(), plugin);
            e.getPlayer().teleport(Lobby.center);
            HungerGames.updateScores(plugin, 0, 0, 0);
        }
        else if (GameStatus.STARTED.isActive() || GameStatus.STARTING.isActive()) {
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            for (int i = 0; i < 9; i ++)
                e.getPlayer().getInventory().setItem(i, HungerGames.compass);
            GameStarter.kills.put(e.getPlayer().getName(), 0);
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
            e.setQuitMessage("[§b!§f] §l" + name + " §fвышел из игры. В живых осталось(ся): §l" + GameStarter.life_players.size());
            closeGame();
            if (!GameStarter.kills.containsKey(name) || GameStarter.kills.get(name).equals(0))
                return;
        }
    }
    @EventHandler
    public void onMove (PlayerMoveEvent e)
    {
        if (!GameStatus.STARTING.isActive())
            return;
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
    public void onInventoryOpenEvent(InventoryOpenEvent e){
        if (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof DoubleChest){
            if (openned_chests.contains(e.getInventory().getLocation()))
                return;
            String name = e.getPlayer().getName();
            if (!GameStarter.life_players.contains(e.getPlayer()))
                return;
            GameStarter.open_chest.replace(name, GameStarter.open_chest.get(name) + 1);
            int min = plugin.getConfig().getInt("range.min");
            int max = plugin.getConfig().getInt("range.max");
            int random_size = (int) (Math.random() * ((max - min) + 1)) + min;
            if (GameStarter.open_chest.get(name) % 4 == 0) {
                chestSetter(HungerGames.good_items, e.getInventory(), random_size);
                GameStarter.open_chest.replace(name, 0);
            } else
                chestSetter(HungerGames.bad_items, e.getInventory(), random_size);
            e.getInventory().addItem(HungerGames.food_items.get(randomGenerator.nextInt(HungerGames.food_items.size())));

            if (e.getPlayer().getInventory().contains(Material.BOW))
                e.getInventory().addItem(new ItemStack(Material.ARROW, (int) (Math.random() * 6)));

            openned_chests.add(e.getInventory().getLocation());
        }
    }
    @EventHandler
    public void onInteract (PlayerInteractEvent e)
    {
        if (GameStatus.WAITING.isActive())
            return;
        if (e.getPlayer().getItemInHand().getType().equals(Material.COMPASS))
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
        if (open.getName().equals("§b§lИгрушка наблюдателя")) //MAYBE ERROR
            PlayerGUI.teamGUIHandler(p, item);
        if (item.getType().equals(Material.SKULL_ITEM))
            e.setCancelled(true);
    }
    @EventHandler
    public void onDeath (PlayerDeathEvent e) {
        Player death = e.getEntity();
        Player killer = death.getKiller();
        try {
            GameStarter.life_players.remove(death);
            e.setDeathMessage("[§b!§f] §l" + killer.getName() + " §fубил §l" + death.getName() + "§f. В живых осталось(ся): §l" + GameStarter.life_players.size());
        } catch (NullPointerException ex)
        {
            e.setDeathMessage("[§b!§f] §l" + death.getName() + " §fумер. В живых осталось(ся): §l" + GameStarter.life_players.size());
        }
        GameStarter.kills.replace(killer.getName(), GameStarter.kills.get(killer.getName()) + 1);
        HungerGames.updateScores(plugin, 0, 0, 0);
        closeGame();
        killer.setFoodLevel(20);
    }
    private void chestSetter (LinkedList<ItemStack> items, Inventory inv, int size)
    {
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
        for (Location loc : HungerListener.openned_chests) {
            loc.getBlock().setType(Material.AIR);
            loc.getBlock().setType(Material.CHEST);
        }
        Player winner = GameStarter.life_players.get(0);
        GameStatus.FINISHING.setActive();

        saveStats();

        Bukkit.broadcastMessage("[§a!§f]§l " + winner.getName() + " §f победил!");
        HungerGames.sendTitle("[§a!§f]", "Победа!");
        Bukkit.broadcastMessage(plugin.getConfig().getString("game.kills_message"));
        for (Player p : Bukkit.getOnlinePlayers())
            Bukkit.broadcastMessage("  *  " + p.getName() + " §fубил §c§l" + GameStarter.kills.get(p.getName()) + "§f игроков(а).");
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
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                String name = p.getName();
                ResultSet rs = HungerGames.statement.executeQuery("SELECT * FROM `TEST` WHERE name = '" + name + "';");
                if (rs.next()) {
                    int new_kills = rs.getInt("kills") + GameStarter.kills.get(name);
                    int new_coins = rs.getInt("gold") + GameStarter.kills.get(name) * 5;
                    int new_deaths = rs.getInt("deaths") + 1;
                    int new_wins = rs.getInt("wins") + 1;
                    if (GameStarter.life_players.contains(p))
                        HungerGames.statement.executeUpdate("UPDATE `TEST` SET kills ='" + new_kills + "', gold = '" + new_coins + "', wins = '" + new_wins + "' WHERE name = '" + name + "';");
                    else
                        HungerGames.statement.executeUpdate("UPDATE `TEST` SET kills ='" + new_kills + "', gold = '" + new_coins + "', deaths = '" + new_deaths + "' WHERE name = '" + name + "';");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    @EventHandler
    public void onRespawn (PlayerRespawnEvent e)
    {
        if (!GameStatus.WAITING.isActive()) {
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            for (int i = 0; i < 9; i ++)
                e.getPlayer().getInventory().setItem(i, HungerGames.compass);
        }
    }
    // ОТМЕНЕННЫЕ
    @EventHandler
    public void onBlockBreak (BlockBreakEvent e)
    {
        e.setCancelled(true);
    }
    @EventHandler
    public void onBlockPlace (BlockPlaceEvent e) { e.setCancelled(true); }
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
