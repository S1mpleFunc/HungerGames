package ru.func.hungergames;

public enum GameStatus {
    WAITING (false),
    STARTING(false),
    STARTED(false),
    FINISHING(false),
    ;
    boolean active;

    GameStatus (boolean active)
    {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
    public void setActive() {
        for (GameStatus g : GameStatus.values())
            g.active = false;
        this.active = true;
    }
}
