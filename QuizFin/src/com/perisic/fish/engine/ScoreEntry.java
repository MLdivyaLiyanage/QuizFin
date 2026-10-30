package com.perisic.fish.engine;

public class ScoreEntry {
    public final String player;
    public final int score;
    public final long time;

    public ScoreEntry(String player, int score, long time) {
        this.player = player;
        this.score = score;
        this.time = time;
    }
}
