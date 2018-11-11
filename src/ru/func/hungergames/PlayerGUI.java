package ru.func.hungergames;

import com.sun.istack.internal.NotNull;

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

    GameStarter gameStarter = new GameStarter();

    public void openPlayerGUI(HungerGames plugin, @NotNull Player p) {
        //Инвентарь с живыми игроками
        Inventory i = Bukkit.createInventory(null, 45,"§b§lИгрушка наблюдателя");

        ItemStack skull = new ItemStack(Material.SKULL_ITEM);
        //Заполнение инвентаря стеклом и головами игроков
        for (int u = 0; u < 9; u++)
            i.setItem(u, plugin.empty);
        for (Player player : new GameStarter().life_players) {
            ItemMeta skull_meta = skull.getItemMeta();
            skull_meta.setDisplayName("§e§l" + player.getName());
            skull_meta.setLore(Arrays.asList("§f- §c§l" + (int) player.getHealth() + " §f§lHP §f-"));
            skull.setItemMeta(skull_meta);
            i.addItem(skull);
        }
        for (int u = 36; u < 45; u++)
            i.setItem(u, plugin.empty);
        //Открытие игрока
        p.openInventory(i);
    }
    private void openVoteFunction (HungerGames plugin, @NotNull Player p, @NotNull Player l)
    {
        Inventory i = Bukkit.createInventory(null, 27, l.getName());

        List<String> lores = new ArrayList<>();
        ItemStack stats = new ItemStack(Material.PAPER);
        ItemMeta im = stats.getItemMeta();

        //Добавление статистики игрока
        lores.add("§fДистрикт: §l" + plugin.playerStats.get(l.getUniqueId()).getDistrict());
        lores.add("§fПобед: §l" + plugin.playerStats.get(l.getUniqueId()).getWins());
        lores.add("§fДенариев: §l" + plugin.playerStats.get(l.getUniqueId()).getCoins());
        lores.add("§fУбийств/Все время: §l" + plugin.playerStats.get(l.getUniqueId()).getKills());
        lores.add("§fK/D: §l" + plugin.playerStats.get(l.getUniqueId()).getKD());
        im.setLore(lores);

        im.setDisplayName("§e§lСтатистика игрока");
        stats.setItemMeta(im);
        //Заполнение инвентаря стеклом и 4 интерактивными предметами
        for (int u = 0; u < 9; u++)
            i.setItem(u, plugin.empty);
        i.setItem(10, stats);
        i.setItem(12, plugin.gold);
        i.setItem(14, plugin.tp);
        i.setItem(16, plugin.inv);
        for (int u = 18; u < 27; u++)
            i.setItem(u, plugin.empty);
        //Открытие инвентаря
        p.openInventory(i);
    }
    public void openRewards (HungerGames plugin, Player p, Player open) {
        Inventory i = Bukkit.createInventory(null, 45, "§e§lНаграды");
        for (int v = 0; v < 9; v++)
            i.setItem(v, plugin.empty);
        if (plugin.playerStats.containsKey(p.getUniqueId())) {
            int n = 9;
            for (String s : plugin.playerStats.get(p.getUniqueId()).getRewards().split(" ")) {
                Rewards r = Rewards.valueOf(s);
                i.setItem(n, plugin.getItem(r.getMaterial(), r.getName(), "§fПолучил " + r.getCause(), 0));
                n = n + 1;
            }
        }
        for (int v = 36; v < 45; v++)
            i.setItem(v, plugin.empty);
        open.openInventory(i);
    }
    public void teamGUIHandler (HungerGames plugin, @NotNull Player p, @NotNull ItemStack item) {
        //Обработка возможных предметов
        if (gameStarter.life_players.contains(p))
            return;
        Material type = item.getType();
        if (type.equals(Material.SKULL_ITEM)) {
            //Открытие инвентаря с интерактивными предметами
            for (Player player : gameStarter.life_players)
                if (player.getName().equals(ChatColor.stripColor(item.getItemMeta().getDisplayName())))
                    openVoteFunction(plugin, p, player);
        } else if (type.equals(Material.EYE_OF_ENDER)) {
            //Телепортирует к выбранному игроку
            for (Player player : gameStarter.life_players)
                if (player.getName().equals(p.getOpenInventory().getTitle()))
                    p.teleport(player.getLocation());
        } else if (type.equals(Material.DIAMOND_BLOCK)) {
            for (Player player : gameStarter.life_players)
                if (player.getName().equals(p.getOpenInventory().getTitle()))
                    openRewards(plugin, player, p);
        } else if (type.equals(Material.CHEST))
        {
            //Открывает инвентарь игрока
            for (Player player : gameStarter.life_players)
                if (player.getName().equals(p.getOpenInventory().getTitle()))
                    p.openInventory(player.getInventory());
        }
    }
}
