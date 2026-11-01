package com.perisic.fish.engine;

import java.util.List;

/** Persistence of Fish Challenge scores (file today, database tomorrow). */
public interface ScoreStore {
    void add(String player, int score);

    List<ScoreEntry> top(int n);

    int best(String player);
}
