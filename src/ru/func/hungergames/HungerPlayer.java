package ru.func.hungergames;

public class HungerPlayer {
    //Создание класса хранящего всю статистику игрока
    int kills;
    int deaths;
    int district;
    int wins;
    int coins;
    String rewards;

    public HungerPlayer (int kills, int deaths, int district, int wins, int coins, String rewards)
    {
        this.kills = kills;
        this.deaths = deaths;
        this.district = district;
        this.wins = wins;
        this.coins = coins;
        this.rewards = rewards;
    }
    public int getKills() {
        return kills;
    }
    public float getKD() {
        return kills / deaths;
    }
    public int getDistrict() {
        return district;
    }
    public int getWins() {
        return wins;
    }
    public int getCoins() {
        return coins;
    }
    public String getRewards() {
        return rewards;
    }
}
