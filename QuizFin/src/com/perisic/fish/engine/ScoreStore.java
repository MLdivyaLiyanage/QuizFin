package com.perisic.fish.engine;

import java.util.List;

/** Persistence of completed game scores and their game modes. */
public interface ScoreStore {
    void add(String player, int score);

    void add(ScoreEntry entry);

    List<ScoreEntry> top(int n);

    int best(String player);
}
