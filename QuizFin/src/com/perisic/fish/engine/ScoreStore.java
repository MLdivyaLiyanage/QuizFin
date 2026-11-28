package com.perisic.fish.engine;

import java.util.List;

/** Persistence of completed game scores and their game modes. */
public interface ScoreStore {
    void add(String player, int score);

    /** @throws ScoreStorageException if the score could not be saved */
    void add(ScoreEntry entry);

    /** @throws ScoreStorageException if scores could not be read */
    List<ScoreEntry> top(int n);

    /** Top individual runs for one mode; null includes all modes. */
    default List<ScoreEntry> top(int n, ScoreEntry.Mode mode) {
        if (mode == null) return top(n);
        return top(Integer.MAX_VALUE).stream()
                .filter(entry -> entry.mode == mode)
                .limit(Math.max(0, n))
                .collect(java.util.stream.Collectors.toList());
    }

    /** Cumulative leaderboard: one row per account and mode, never across modes. */
    default List<ScoreEntry> totals(int n, ScoreEntry.Mode mode) {
        record Key(String player, ScoreEntry.Mode mode) { }
        var grouped = new java.util.HashMap<Key, ScoreEntry>();
        for (ScoreEntry entry : top(Integer.MAX_VALUE, mode)) {
            Key key = new Key(entry.player.toLowerCase(java.util.Locale.ROOT), entry.mode);
            grouped.merge(key, entry, (a, b) -> new ScoreEntry(a.player,
                    a.quizScore == null && b.quizScore == null ? null
                            : (a.quizScore == null ? 0 : a.quizScore) + (b.quizScore == null ? 0 : b.quizScore),
                    a.fishScore + b.fishScore, Math.max(a.time, b.time), a.mode));
        }
        return grouped.values().stream()
                .sorted(java.util.Comparator.comparingInt((ScoreEntry e) -> e.score).reversed()
                        .thenComparingLong(e -> e.time)
                        .thenComparing(e -> e.player.toLowerCase(java.util.Locale.ROOT))
                        .thenComparing(e -> e.mode.name()))
                .limit(Math.max(0, n))
                .collect(java.util.stream.Collectors.toList());
    }

    int best(String player);
}
