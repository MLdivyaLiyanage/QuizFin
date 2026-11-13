package com.perisic.fish.engine;

// AI-assisted corrections (OpenAI Codex, 2026-11-11): propagate storage failures
// and include legacy mixed-case player names in personal bests. See SOURCES.md.

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Leaderboard stored in PostgreSQL using a TIMESTAMP played_at column.
 */
public class Leaderboard implements ScoreStore {

    private final String url;
    private final String user;
    private final String password;

    public Leaderboard() {
        Properties db = loadDbProperties();
        this.url = db.getProperty("db.url", "jdbc:postgresql://localhost:5432/quizfin");
        this.user = db.getProperty("db.user", "postgres");
        this.password = db.getProperty("db.password", "");
        ensureTable();
    }

    @Override
    public synchronized void add(String player, int score) {
        add(new ScoreEntry(player, null, score, System.currentTimeMillis()));
    }

    @Override
    public synchronized void add(ScoreEntry entry) {
        String sql = "INSERT INTO scores (player, score, quiz_score, fish_score, mode, played_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entry.player == null ? "Guest" : entry.player);
            ps.setInt(2, entry.score);
            if (entry.quizScore == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, entry.quizScore);
            }
            ps.setInt(4, entry.fishScore);
            ps.setString(5, entry.mode.name());
            ps.setTimestamp(6, Timestamp.from(Instant.ofEpochMilli(entry.time)));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ScoreStorageException("Could not save score", e);
        }
    }

    @Override
    public synchronized List<ScoreEntry> top(int n) {
        List<ScoreEntry> out = new ArrayList<>();
        String sql = "SELECT player, quiz_score, fish_score, mode, played_at "
                + "FROM scores ORDER BY score DESC, played_at ASC LIMIT ?";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(0, n));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer quizScore = rs.getObject("quiz_score") == null
                            ? null
                            : rs.getInt("quiz_score");
                    Timestamp playedAt = rs.getTimestamp("played_at");
                    long time = playedAt == null ? System.currentTimeMillis() : playedAt.getTime();
                    out.add(new ScoreEntry(
                            rs.getString("player"),
                            quizScore,
                            rs.getInt("fish_score"),
                            time,
                            ScoreEntry.Mode.valueOf(rs.getString("mode"))));
                }
            }

            out.sort(Comparator.comparingInt((ScoreEntry e) -> e.score).reversed()
                    .thenComparingLong(e -> e.time));
        } catch (SQLException e) {
            throw new ScoreStorageException("Could not read leaderboard", e);
        }

        return out;
    }

    @Override
    public synchronized int best(String player) {
        String sql = "SELECT COALESCE(MAX(score), 0) AS best FROM scores WHERE LOWER(player) = LOWER(?)";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player == null ? "Guest" : player);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("best");
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not read best score: " + e.getMessage());
        }
        return 0;
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS scores ("
                + "id SERIAL PRIMARY KEY, "
                + "player VARCHAR(64) NOT NULL, "
                + "score INTEGER NOT NULL, "
                + "quiz_score INTEGER NULL, "
                + "fish_score INTEGER NOT NULL, "
                + "mode VARCHAR(20) NOT NULL, "
                + "played_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Could not ensure scores table: " + e.getMessage());
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private Properties loadDbProperties() {
        Properties p = new Properties();
        Path custom = Paths.get(System.getProperty("quizfin.db.properties", "db.properties"));
        Path home = Paths.get(System.getProperty("user.home"), ".quizfin", "db.properties");

        for (Path path : new Path[] { custom, home }) {
            if (Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    p.load(in);
                } catch (IOException e) {
                    System.err.println("Could not read " + path + ": " + e.getMessage());
                }
                break;
            }
        }

        String envUrl = System.getenv("QUIZFIN_DB_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            p.setProperty("db.url", envUrl.trim());
        }
        String envUser = System.getenv("QUIZFIN_DB_USER");
        if (envUser != null && !envUser.isBlank()) {
            p.setProperty("db.user", envUser.trim());
        }
        String envPassword = System.getenv("QUIZFIN_DB_PASSWORD");
        if (envPassword != null && !envPassword.isBlank()) {
            p.setProperty("db.password", envPassword.trim());
        }

        return p;
    }
}
