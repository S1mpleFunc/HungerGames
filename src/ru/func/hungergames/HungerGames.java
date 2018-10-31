package ru.func.hungergames;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class HungerGames extends JavaPlugin {

    static ScoreboardManager manager;

    public static LinkedList<ItemStack> bad_items = new LinkedList<>();
    public static LinkedList<ItemStack> good_items = new LinkedList<>();
    public static LinkedList<ItemStack> food_items = new LinkedList<>();
    public static List<String> scores = new ArrayList<>();
    public static ItemStack compass = new ItemStack(Material.COMPASS);
    public static HashMap<UUID, HungerPlayer> playerStats = new HashMap<>();

    private ItemMeta compass_meta = compass.getItemMeta();
    static Statement statement;

    public static ItemStack empty;
    public static ItemStack gold;
    public static ItemStack tp;
    public static ItemStack inv;
    @Override
    public void onEnable()
    {
        registerConfig();
        //Определение предметов, в компасе наблюдателя (кроме бумаги со статистикой(т. к. она не общая, а личная)), и серого стекла
        gold = getItem(Material.GOLD_INGOT, "§e§lСпонсировать игрока", getConfig().getString("lores.sponsor"));
        tp = getItem(Material.EYE_OF_ENDER, "§e§lТелепортироваться к игроку", getConfig().getString("lores.teleport"));
        inv = getItem(Material.CHEST, "§e§lИнвентарь игрока", getConfig().getString("lores.inventory"));

        empty = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
        ItemMeta emptyMeta = empty.getItemMeta();
        emptyMeta.setDisplayName("/");
        empty.setItemMeta(emptyMeta);

        World world = Bukkit.getWorld(getConfig().getString("lobby.world"));

        manager = Bukkit.getScoreboardManager();

        compass_meta.setDisplayName(getConfig().getString("compass.name"));
        compass.setItemMeta(compass_meta);
        scores.addAll(getConfig().getStringList("score"));
        //Подключение обработчиков событий и команд
        getCommand("center").setExecutor(new HungerCommands(this));
        Bukkit.getPluginManager().registerEvents(new HungerListener(this), this);
        //Подкоючение к базе данных
        MySql base = new MySql(getConfig().getString("user"), getConfig().getString("password"), getConfig().getString("host"), getConfig().getString("database"), getConfig().getInt("port"));
        try {
            getLogger().info("[!] Connecting to DataBase.");
            statement = base.openConnection().createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `TEST` (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid TEXT, name TEXT, gold INT, district INT, kills INT, wins INT, deaths INT);");
            getLogger().info("[!] Connected to DataBase.");
        } catch (ClassNotFoundException | SQLException e)
        {
            getLogger().info("[!] Connection exception.");
        }
        //Загрузка, создание игрока
        for (Player p : Bukkit.getOnlinePlayers()) {
            loadStats(p, this);
            hashStats(p.getUniqueId());
            p.setFoodLevel(20);
        }
        //Запуск лобби
        Lobby.waitLobby(this);
        toItemStack("random.bad_items", bad_items);
        toItemStack("random.good_items", good_items);
        toItemStack("random.food", food_items);
        updateScores(this, 0, 0, 0);
        //Настройки мира
        world.getWorldBorder().setSize(getConfig().getInt("game.default_size"));
        Bukkit.setSpawnRadius(0);
        world.setAutoSave(false);
        world.setDifficulty(Difficulty.HARD);
        world.setMonsterSpawnLimit(0);
        world.getEntities().clear();

        getLogger().info(getConfig().getString("name") + " был запущен.");
    }
    public static void loadStats (Player p, HungerGames plugin)
    {
        //Попытка загрузки и выведения информации о игроке
        try {
            ResultSet rs = statement.executeQuery("SELECT * FROM `TEST` WHERE uuid = '" + p.getUniqueId() + "';");
            if (!rs.next()) {
                statement.executeUpdate("INSERT INTO `TEST` (uuid, name, gold, district, kills, wins, deaths) VALUES('" + p.getUniqueId() + "', '" + p.getName() + "', 0, 13, 0, 0, 1);");
                p.sendMessage(plugin.getConfig().getString("profile.new"));
            }
            else
                p.sendMessage(plugin.getConfig().getString("profile.connected"));
            p.sendMessage("[§b!§f] §bЗапись: §f" + p.getName()
                    + ", §bваш номер: §f" + rs.getString("id")
                    + "\nВсего убийств: §c§l" + rs.getInt("kills")
                    + "§f, побед: §e§l" + rs.getInt("wins")
                    + "§f, k/d: §l" + ((float) rs.getInt("kills") / (float) rs.getInt("deaths"))
            );
        } catch (SQLException ex)
        {
            p.sendMessage(plugin.getConfig().getString("profile.exception"));
        }
    }
    public static void sendTitle (String message, String label)
    {
        for (Player p : Bukkit.getOnlinePlayers())
            p.sendTitle(message, label);
    }
    private void registerConfig ()
    {
        getConfig().options().copyDefaults(true);
        saveConfig();
    }
    private void toItemStack (String s, LinkedList<ItemStack> llist)
    {
        for (String d : getConfig().getStringList(s))
            llist.add(new ItemStack(Material.valueOf(d)));
    }
    public static void updateScores (HungerGames plugin, int waiting, int chest, int death)
    {
        //Обновление SCOREBOARD для каждого игрока
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = manager.getNewScoreboard();
            Objective objective = board.registerNewObjective("scoreboard","dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(plugin.getConfig().getString("name"));
            for (String s : p.getScoreboard().getEntries())
                p.getScoreboard().resetScores(s);
            LinkedList<Score> discores = new LinkedList<>();
            discores.add(objective.getScore(scores.get(9) + "§f(§e" + Bukkit.getOnlinePlayers().size() + "§f/§e" + plugin.getConfig().getInt("people_need") + "§f)"));
            discores.add(objective.getScore("  "));
            if (waiting != 0 && GameStatus.WAITING.isActive())
                discores.add(objective.getScore(scores.get(2) + waiting));
            else if (chest != 0)
                discores.add(objective.getScore(scores.get(3) + chest));
            else if (death != 0)
                discores.add(objective.getScore(scores.get(4) + death));
            discores.add(objective.getScore(scores.get(1)));
            discores.add(objective.getScore(" "));
            if (!GameStatus.WAITING.isActive()) {
                discores.set(0, objective.getScore(scores.get(0) + "§f(§e" + GameStarter.life_players.size() + "§f/§e" + Bukkit.getOnlinePlayers().size() + "§f)"));
                if (GameStarter.kills.containsKey(p.getName()))
                    discores.add(objective.getScore(scores.get(5) + GameStarter.kills.get(p.getName())));
            } else
                discores.add(objective.getScore(scores.get(10) + HungerGames.playerStats.get(p.getUniqueId()).getKills()));
            discores.add(objective.getScore(scores.get(6) + HungerGames.playerStats.get(p.getUniqueId()).getCoins()));
            discores.add(objective.getScore(scores.get(7) + HungerGames.playerStats.get(p.getUniqueId()).getWins()));
            discores.add(objective.getScore(scores.get(8) + HungerGames.playerStats.get(p.getUniqueId()).getDistrict()));

            for (Score s : discores)
                s.setScore(discores.indexOf(s));
            discores.clear();
            p.setScoreboard(board);
        }
    }
    public static ItemStack getItem (Material material, String name, String lore)
    {
        ItemStack is = new ItemStack(material);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(name);
        im.setLore(Arrays.asList(lore));
        is.setItemMeta(im);
        return is;
    }
    public static void hashStats (UUID uid)
    {
        //Создание экземпляра класса HungerPlayer, для игрока с uuid ?= uid
        try {
            ResultSet rs = HungerGames.statement.executeQuery("SELECT * FROM `TEST` WHERE uuid = '" + uid + "';");
            if (rs.next()) {
                HungerGames.playerStats.put(uid, new HungerPlayer(
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getInt("district"),
                        rs.getInt("wins"),
                        rs.getInt("gold")
                ));
            }
        } catch (SQLException ex) {}
    }
}