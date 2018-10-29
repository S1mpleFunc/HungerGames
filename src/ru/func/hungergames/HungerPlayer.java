package ru.func.hungergames;

public class HungerPlayer {
    //Создание класса хранящего всю статистики игрока
    int kills;
    int deaths;
    int district;
    int wins;
    int coins;
    public HungerPlayer (int kills, int deaths, int district, int wins, int coins)
    {
        this.kills = kills;
        this.deaths = deaths;
        this.district = district;
        this.wins = wins;
        this.coins = coins;
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
}
