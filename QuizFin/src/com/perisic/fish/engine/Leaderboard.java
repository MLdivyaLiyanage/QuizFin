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
 * Fish Challenge leaderboard, stored as a CSV file (score,timestamp,player) in
 * the user's home directory.
 */
public class Leaderboard implements ScoreStore {

    private final Path file = Paths.get(System.getProperty("user.home"), "quizfin_leaderboard.csv");

    @Override
    public synchronized void add(String player, int score) {
        String name = (player == null ? "Guest" : player).replace('\n', ' ').replace('\r', ' ');
        String line = score + "," + System.currentTimeMillis() + "," + name + System.lineSeparator();
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
                String[] p = line.split(",", 3);
                if (p.length < 3)
                    continue;
                try {
                    all.add(new ScoreEntry(p[2], Integer.parseInt(p[0]), Long.parseLong(p[1])));
                } catch (NumberFormatException ignored) {
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
        all.sort(Comparator.comparingInt((ScoreEntry e) -> -e.score).thenComparingLong(e -> e.time));
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
