package ru.func.hungergames;

import org.bukkit.Material;

public enum Rewards {
    ALPHA_TEST("§eОдин из первых.", "за участие в §lALPHA§f тестировании", Material.GOLD_AXE),
    OLD_PLAYER("§eВерный своему делу", "за долгую непрерывную игру", Material.WATCH),

    KILL_LEAD("§c§lУбийца богов", "за самое большое количество убийств за все время", Material.DIAMOND_SWORD),
    TOP10_KILL("§c§lУбийца чемпионов", "за §lTOP10§f убийств за все время", Material.IRON_SWORD),
    TOP25_KILL("§cУбийца убийц", "за §lTOP25§f убийств за все время", Material.GOLD_SWORD),

    WINNER_WINTER_SEASON_18("§b§lЛедяной лорд 2018-2019", "за победу в §bЗимнем сезоне", Material.DIAMOND),
    WINNER_AUTUMN_SEASON_19("§6§lЛорд леса 2019", "за победу в §6Весеннем сезоне", Material.DIAMOND),

    TOP10_WINTER_SEASON_18("§b§lЛедяной чемпион 2018-2019", "за §lTOP10§f в §bЗимнем сезоне", Material.ICE),
    TOP10_AUTUMN_SEASON_19("§6§lЛиственный чемпион 2019", "за §lTOP10§f в §6Весеннем сезоне", Material.LEAVES)
    ;

    String name;
    String cause;
    Material material;

    Rewards (String name, String cause, Material material) {
        this.name = name;
        this.cause = cause;
        this.material = material;
    }
    public String getName() {
        return name;
    }
    public Material getMaterial() {
        return material;
    }
    public String getCause() {
        return cause;
    }
}
