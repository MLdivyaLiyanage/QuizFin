package com.perisic.fish.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Properties;

/**
 * JDBC access to the PostgreSQL {@code users} table.
 *
 * <p>Every method opens and closes its own short-lived connection. Nothing is
 * cached, so restarting PostgreSQL can never leave the app holding a dead
 * connection: the next login simply reconnects.</p>
 *
 * <p>Only {@link AuthService} uses this class; it is package-private on
 * purpose so passwords and hashes never leave the auth layer.</p>
 */
final class UserRepository {

    /** The timestamp the row was created, if known; {@code null} otherwise. */
    private static final String SELECT_ROW =
           "SELECT username, display_name, password_hash, "
           + "created_at, last_login FROM users WHERE username = ?";

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS users ("
           + "username      VARCHAR(20) PRIMARY KEY, "
           + "display_name  VARCHAR(64) NOT NULL DEFAULT '', "
           + "password_hash TEXT        NOT NULL, "
           + "created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP, "
           + "last_login    TIMESTAMP   NULL)";

    private static final String CREATE_INDEX =
           "CREATE INDEX IF NOT EXISTS idx_users_last_login ON users (last_login DESC)";

    /** One row of the users table, without the timestamp columns. */
    static final class Account {
        final String username;
        final String displayName;
        final String passwordHash;

        Account(String username, String displayName, String passwordHash) {
            this.username = username;
            this.displayName = displayName;
            this.passwordHash = passwordHash;
        }
    }

    private final String url;
    private final String user;
    private final String password;

    UserRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Builds a repository from {@code db.properties} style settings. */
    static UserRepository from(Properties db) {
        String url = value(db, "db.url", "jdbc:postgresql://localhost:5432/quizfin");
        String user = value(db, "db.user", "postgres");
        String password = value(db, "db.password", "");
        return new UserRepository(url, user, password);
    }

    /** Creates the table on first run; safe to call every startup. */
    void ensureTable() throws SQLException {
        try (Connection conn = open();
             Statement s = conn.createStatement()) {
            s.executeUpdate(CREATE_TABLE);
            s.executeUpdate(CREATE_INDEX);
        }
    }

    void insert(String username, String displayName, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (username, display_name, password_hash, created_at) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, displayName);
            ps.setString(3, passwordHash);
            ps.setTimestamp(4, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    /** @return the account, or {@code null} when that username does not exist */
    Account find(String username) throws SQLException {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(SELECT_ROW)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Account(
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("password_hash"));
            }
        }
    }

    /**
     * @return identity and time metadata for a user, or {@code null} when unknown.
     *         Passwords are deliberately not exposed.
     */
    AccountInfo accountInfo(String username) throws SQLException {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(SELECT_ROW)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Timestamp created = rs.getTimestamp("created_at");
                Timestamp lastLogin = rs.getTimestamp("last_login");
                return new AccountInfo(
                        rs.getString("username"),
                        rs.getString("display_name"),
                        created == null ? null : created.toInstant(),
                        lastLogin == null ? null : lastLogin.toInstant());
            }
        }
    }

    /** Upgrades a hash only if it has not changed since login read it. */
    void updatePasswordHash(String username, String previousHash, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ? AND password_hash = ?";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, username);
            ps.setString(3, previousHash);
            ps.executeUpdate();
        }
    }

    boolean exists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    void setLastLogin(String username) throws SQLException {
        String sql = "UPDATE users SET last_login = ? WHERE username = ?";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static String value(Properties db, String key, String fallback) {
        String v = db.getProperty(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
