package ru.func.hungergames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerGUI {
    public static void openPlayerGUI(Player p) {
        Inventory i = Bukkit.createInventory(null, 45,"§b§lИгрушка наблюдателя");

        ItemStack empty = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
        ItemMeta emptyMeta = empty.getItemMeta();
        emptyMeta.setDisplayName("/");
        empty.setItemMeta(emptyMeta);

        ItemStack skull = new ItemStack(Material.SKULL_ITEM);

        for (int u = 0; u < 9; u++)
            i.setItem(u, empty);
        for (Player player : GameStarter.life_players) {
            ItemMeta skull_meta = skull.getItemMeta();
            skull_meta.setDisplayName("§e§l" + player.getName());
            skull_meta.setLore(Arrays.asList("§f- §c§l" + (int) player.getHealth() + " §f§lHP §f-"));
            skull.setItemMeta(skull_meta);
            i.addItem(skull);
        }
        for (int u = 36; u < 45; u++)
            i.setItem(u, empty);

        p.openInventory(i);
    }
    public static void teamGUIHandler (Player p, ItemStack item)
    {
        if (GameStarter.life_players.contains(p))
            return;
        Material type = item.getType();
        if (type.equals(Material.SKULL_ITEM))
        {
            for (Player player : GameStarter.life_players)
                if (player.getName().equals(ChatColor.stripColor(item.getItemMeta().getDisplayName())))
                    openVoteFunction(p, player);
        } else if (type.equals(Material.EYE_OF_ENDER)) {
            for (Player player : GameStarter.life_players)
                if (player.getName().equals(p.getOpenInventory().getTitle()))
                    p.teleport(player.getLocation());
        } else if (type.equals(Material.GOLD_INGOT))
        {
        } else if (type.equals(Material.CHEST))
        {
            for (Player player : GameStarter.life_players)
                if (player.getName().equals(p.getOpenInventory().getTitle()))
                    p.openInventory(player.getInventory());
        }
    }
    private static void openVoteFunction (Player p, Player l)
    {
        Inventory i = Bukkit.createInventory(null, 27, l.getName());

        ItemStack empty = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
        ItemMeta emptyMeta = empty.getItemMeta();
        emptyMeta.setDisplayName("/");
        empty.setItemMeta(emptyMeta);

        List<String> lores = new ArrayList<>();
        ItemStack stats = new ItemStack(Material.PAPER);
        ItemMeta im = stats.getItemMeta();

        lores.add("§fДистрикт: §l" + HungerGames.playerStats.get(l.getUniqueId()).getDistrict());
        lores.add("§fПобед: §l" + HungerGames.playerStats.get(l.getUniqueId()).getWins());
        lores.add("§fДенариев: §l" + HungerGames.playerStats.get(l.getUniqueId()).getCoins());
        lores.add("§fУбийств/Все время: §l" + HungerGames.playerStats.get(l.getUniqueId()).getKills());
        lores.add("§fУбийств: §l" + GameStarter.kills.get(l.getName()));
        lores.add("§fK/D: §l" + HungerGames.playerStats.get(l.getUniqueId()).getKD());
        im.setLore(lores);

        im.setDisplayName("§e§lСтатистика игрока");
        stats.setItemMeta(im);
        for (int u = 0; u < 9; u++)
            i.setItem(u, empty);
        i.setItem(10, stats);
        i.setItem(12, HungerGames.gold);
        i.setItem(14, HungerGames.tp);
        i.setItem(16, HungerGames.inv);
        for (int u = 18; u < 27; u++)
            i.setItem(u, empty);

        p.openInventory(i);
    }
    private static void openSponsoeMenu (Player p, Player l)
    {
        Inventory i = Bukkit.createInventory(null, 27, l.getName());

        ItemStack empty = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
        ItemMeta emptyMeta = empty.getItemMeta();
        emptyMeta.setDisplayName("/");
        empty.setItemMeta(emptyMeta);
        for (int u = 0; u < 9; u++)
            i.setItem(u, empty);
        i.setItem(12, HungerGames.gold);
        i.setItem(14, HungerGames.tp);
        for (int u = 18; u < 27; u++)
            i.setItem(u, empty);

        p.openInventory(i);
    }
}
