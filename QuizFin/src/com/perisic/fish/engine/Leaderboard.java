package com.perisic.fish.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Leaderboard stored in the user's home directory. Reads original fish-score
 * records and versioned records containing game mode and both scores.
 */
public class Leaderboard implements ScoreStore {

    private final Path file;

    public Leaderboard() {
        this(Paths.get(System.getProperty("user.home"), "quizfin_leaderboard.csv"));
    }

    public Leaderboard(Path file) {
        this.file = file;
    }

    @Override
    public synchronized void add(String player, int score) {
        add(new ScoreEntry(player, null, score, System.currentTimeMillis()));
    }

    @Override
    public synchronized void add(ScoreEntry entry) {
        String name = (entry.player == null ? "Guest" : entry.player).replace('\n', ' ').replace('\r', ' ');
        String line = "v2," + entry.fishScore + "," + entry.time + "," + entry.mode.name() + ","
                + (entry.quizScore == null ? "" : entry.quizScore) + "," + name + System.lineSeparator();
        try {
            Files.write(file, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Could not save score: " + e);
        }
    }

    private List<ScoreEntry> readAll() {
        List<ScoreEntry> all = new ArrayList<>();
        if (!Files.exists(file)) {
            return all;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                try {
                    if (line.startsWith("v2,")) {
                        String[] p = line.split(",", 6);
                        if (p.length < 6) continue;
                        ScoreEntry.Mode mode = ScoreEntry.Mode.valueOf(p[3]);
                        int fish = Integer.parseInt(p[1]);
                        long time = Long.parseLong(p[2]);
                        if (mode == ScoreEntry.Mode.UNKNOWN) {
                            all.add(new ScoreEntry(p[5], fish, time));
                        } else {
                            Integer quiz = mode == ScoreEntry.Mode.QUIZ_AND_FISH ? Integer.valueOf(p[4]) : null;
                            all.add(new ScoreEntry(p[5], quiz, fish, time));
                        }
                    } else {
                        String[] p = line.split(",", 3);
                        if (p.length < 3) continue;
                        all.add(new ScoreEntry(p[2], Integer.parseInt(p[0]), Long.parseLong(p[1])));
                    }
                } catch (IllegalArgumentException ignored) {
                    // skip corrupt line
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read leaderboard: " + e);
        }
        return all;
    }

    /** Top n scores, highest first (older entry wins a tie). */
    @Override
    public synchronized List<ScoreEntry> top(int n) {
        List<ScoreEntry> all = readAll();
        all.sort(Comparator.comparingInt((ScoreEntry e) -> e.score).reversed().thenComparingLong(e -> e.time));
        return all.subList(0, Math.min(n, all.size()));
    }

    /** Best score of a player, or 0. */
    @Override
    public synchronized int best(String player) {
        int best = 0;
        for (ScoreEntry e : readAll()) {
            if (e.player.equals(player)) {
                best = Math.max(best, e.score);
            }
        }
        return best;
    }
}
