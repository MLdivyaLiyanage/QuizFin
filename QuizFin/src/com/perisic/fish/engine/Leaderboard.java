package com.perisic.fish.engine;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import org.postgresql.ds.PGSimpleDataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    // there use the introperbility and there connect with the PostgreSQL and store the scores in db
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
        return top(n, null);
    }

    @Override
    public synchronized List<ScoreEntry> top(int n, ScoreEntry.Mode mode) {
        String sql = "SELECT player, quiz_score, fish_score, mode, played_at "
                + "FROM scores " + (mode == null ? "" : "WHERE mode = ? ")
                + "ORDER BY score DESC, played_at ASC, id ASC LIMIT ?";
        return readScores(sql, n, mode);
    }

    @Override
    public synchronized List<ScoreEntry> totals(int n, ScoreEntry.Mode mode) {
        String sql = "SELECT LOWER(player) AS player, SUM(quiz_score) AS quiz_score, "
                + "SUM(fish_score) AS fish_score, mode, MAX(played_at) AS played_at "
                + "FROM scores " + (mode == null ? "" : "WHERE mode = ? ")
                + "GROUP BY LOWER(player), mode "
                + "ORDER BY SUM(COALESCE(quiz_score, 0) + fish_score) DESC, "
                + "MAX(played_at) ASC, LOWER(player) ASC, mode ASC LIMIT ?";
        return readScores(sql, n, mode);
    }

    private List<ScoreEntry> readScores(String sql, int n, ScoreEntry.Mode mode) {
        List<ScoreEntry> out = new ArrayList<>();
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int parameter = 1;
            if (mode != null) ps.setString(parameter++, mode.name());
            ps.setInt(parameter, Math.max(0, n));

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
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException missing) {
            // invalid_catalog_name: do not attempt DDL for authentication or network failures.
            if (!"3D000".equals(missing.getSQLState())) {
                throw missing;
            }
            try {
                createDatabase(url, user, password);
            } catch (SQLException failure) {
                SQLException error = new SQLException(
                        "Could not automatically create the configured database. "
                        + "The db.user role must be able to connect to postgres and have CREATEDB permission. "
                        + failure.getMessage(), failure.getSQLState(), failure);
                error.addSuppressed(missing);
                throw error;
            }
            return DriverManager.getConnection(url, user, password);
        }
    }

    private synchronized void createDatabase(String url, String user, String password)
            throws SQLException {
        // Use the driver's URL parser to preserve hosts, ports, SSL and other options.
        PGSimpleDataSource maintenance = new PGSimpleDataSource();
        maintenance.setUrl(url);
        String database = maintenance.getDatabaseName();
        if (database == null || database.isBlank() || database.indexOf('\0') >= 0) {
            throw new SQLException("Invalid database name in db.url", "3D000");
        }
        maintenance.setDatabaseName("postgres");
        try (Connection connection = maintenance.getConnection(user, password);
             Statement statement = connection.createStatement()) {
            // Database identifiers cannot be bound as parameters; quote embedded quotes.
            statement.executeUpdate("CREATE DATABASE \"" + database.replace("\"", "\"\"") + "\"");
        } catch (SQLException failure) {
            // Another app instance may have created the same database in the meantime.
            if (!"42P04".equals(failure.getSQLState()) && !"23505".equals(failure.getSQLState())) {
                throw failure;
            }
        }
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
