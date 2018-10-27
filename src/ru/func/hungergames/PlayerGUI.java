package ru.func.hungergames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PlayerGUI {
    public static void openPlayerGUI(Player p) {
        Inventory i = Bukkit.createInventory(null, 45,"§b§lИгрушка наблюдателя");

        ItemStack empty = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
        ItemMeta emptyMeta = empty.getItemMeta();
        emptyMeta.setDisplayName("§1/");
        empty.setItemMeta(emptyMeta);

        ItemStack skull = new ItemStack(Material.SKULL_ITEM);

        for (int u = 0; u < 9; u++)
            i.setItem(u, empty);
        for (Player player : GameStarter.life_players) {
            ItemMeta skull_meta = skull.getItemMeta();
            skull_meta.setDisplayName("§e§l" + player.getName());
            skull_meta.setLore(Arrays.asList("- §c§l" + player.getHealth() + " §f§lHP §f-"));
            skull.setItemMeta(skull_meta);
            i.addItem(skull);
        }
        for (int u = 36; u < 45; u++)
            i.setItem(u, empty);

        p.openInventory(i);
    }
    public static void teamGUIHandler (Player p, ItemStack item)
    {
        Player point = p;
        for (Player player : GameStarter.life_players)
            if (player.getName().equals(ChatColor.stripColor(item.getItemMeta().getDisplayName())))
                point = player;
        p.teleport(point.getLocation());
    }
}
