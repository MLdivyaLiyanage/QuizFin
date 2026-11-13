package com.perisic.fish.engine;

// AI-assisted contract documentation (OpenAI Codex, 2026-11-11). See SOURCES.md.

import java.util.List;

/** Persistence of completed game scores and their game modes. */
public interface ScoreStore {
    void add(String player, int score);

    /** @throws ScoreStorageException if the score could not be saved */
    void add(ScoreEntry entry);

    /** @throws ScoreStorageException if scores could not be read */
    List<ScoreEntry> top(int n);

    int best(String player);
}
