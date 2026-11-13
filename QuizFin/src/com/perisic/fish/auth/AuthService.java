package com.perisic.fish.auth;

// AI-assisted corrections (OpenAI Codex, 2026-11-11): canonical identity,
// remembered-account validation, locale handling, schema retry, and removal of
// automatic hardcoded credentials. See SOURCES.md for the assistance record.

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Properties;

import com.perisic.fish.auth.AccountInfo;

/**
 * Authentication facade used by the GUI: register, login, resume ("remember
 * me" cookie) and logout.
 *
 * <p>Accounts are rows in the PostgreSQL {@code users} table (username,
 * display name, bcrypt hash, timestamps) instead of {@code users.properties}.
 * Passwords are never stored in plain text: they are salted and hashed with
 * {@link PasswordHasher} (bcrypt, cost 12).</p>
 *
 * <p>Connection settings come from {@code db.properties} in the working
 * directory, then {@code ~/.quizfin/db.properties}; the environment variables
 * {@code QUIZFIN_DB_URL}, {@code QUIZFIN_DB_USER} and
 * {@code QUIZFIN_DB_PASSWORD} win over both.</p>
 *
 * <pre>
 * db.url=jdbc:postgresql://localhost:5432/quizfin
 * db.user=postgres
 * db.password=your-password
 * </pre>
 *
 * <p>Sessions (the "remember me" cookie) stay in the user's data folder and are
 * handled by {@link SessionManager}, so nothing else in the app changes.</p>
 */
public class AuthService {
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 30_000;

    private final UserRepository users;
    private final SessionManager sessions;
    private boolean schemaReady;
    private final Map<String, int[]> fails = new HashMap<>();      // user -> {count}
    private final Map<String, Long> lockedUntil = new HashMap<>();

    public AuthService() {
        this(Paths.get(System.getProperty("quizfin.dir",
                Paths.get(System.getProperty("user.home"), ".quizfin").toString())),
                loadDbSettings());
    }

    /** Package-private: lets a smoke test inject a data folder and db settings. */
    AuthService(Path dir, Properties db) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Could not create " + dir + ": " + e);
        }
        this.users = UserRepository.from(db);
        this.sessions = new SessionManager(dir);
        ensureSchema();
    }

    /**
     * Creates the {@code users} table. A stopped
     * database server must not stop the app from opening, so failures are only
     * reported and retried on the next login.
     */
    private void ensureSchema() {
        try {
            ensureSchemaReady();
        } catch (SQLException e) {
            System.err.println("Users table not ready (" + e.getMessage()
                    + "). Start PostgreSQL and check db.properties.");
        }
    }

    private void ensureSchemaReady() throws SQLException {
        if (!schemaReady) {
            users.ensureTable();
            schemaReady = true;
        }
    }

    /** Creates an account. Username 3-20 letters/digits/_, password 6+ chars. */
    public synchronized void register(String username, char[] password) throws AuthException {
        String name = username == null ? "" : username.trim();
        if (!name.matches("[A-Za-z0-9_]{3,20}")) {
            throw new AuthException("Username must be 3-20 letters, digits or _.");
        }
        if (password == null || password.length < 6) {
            throw new AuthException("Password must be at least 6 characters.");
        }
        if (!PasswordHasher.fitsBcrypt(password)) {
            throw new AuthException("Password must be at most 72 UTF-8 bytes.");
        }
        String key = name.toLowerCase(Locale.ROOT);
        try {
            ensureSchemaReady();
            if (users.exists(key)) {
                throw new AuthException("That username is already taken.");
            }
            users.insert(key, name, PasswordHasher.hash(password));
        } catch (SQLException e) {
            System.err.println("Could not create account: " + e.getMessage());
            throw new AuthException("Could not save your account. Is PostgreSQL running?");
        }
    }

    /** Checks credentials and starts a session (cookie written if remember). */
    public synchronized Session login(String username, char[] password, boolean remember) throws AuthException {
        String key = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Long until = lockedUntil.get(key);
        if (until != null && until > now) {
            throw new AuthException("Too many attempts. Try again in " + ((until - now) / 1000 + 1) + " s.");
        }
        UserRepository.Account account;
        try {
            ensureSchemaReady();
            account = users.find(key);
        } catch (SQLException e) {
            System.err.println("Could not read users: " + e.getMessage());
            throw new AuthException("Could not reach the account database. Is PostgreSQL running?");
        }
        boolean ok = account != null && PasswordHasher.verify(password, account.passwordHash);
        if (!ok) {
            int[] c = fails.computeIfAbsent(key, k -> new int[1]);
            if (++c[0] >= MAX_FAILS) {
                lockedUntil.put(key, now + LOCK_MS);
                c[0] = 0;
            }
            throw new AuthException("Wrong Password / Username");
        }
        fails.remove(key);
        lockedUntil.remove(key);
        if (PasswordHasher.needsRehash(account.passwordHash) && PasswordHasher.fitsBcrypt(password)) {
            try {
                users.updatePasswordHash(key, account.passwordHash, PasswordHasher.hash(password));
            } catch (SQLException e) {
                System.err.println("Could not upgrade password hash: " + e.getMessage());
            }
        }
        try {
            users.setLastLogin(key);
        } catch (SQLException e) {
            System.err.println("Could not record last login: " + e.getMessage());
        }
        // Virtual identity uses the database primary key, not a presentation label.
        return sessions.issue(account.username, remember);
    }

    /** Auto-login from the "remember me" cookie, or null. */
    public synchronized Session resume() {
        Session saved = sessions.resume();
        if (saved == null) return null;
        try {
            ensureSchemaReady();
            UserRepository.Account account = users.find(saved.username.trim().toLowerCase(Locale.ROOT));
            if (account == null) {
                sessions.revoke(saved.token);
                return null;
            }
            // Also normalizes older sessions that stored the mixed-case display name.
            return new Session(account.username, saved.token, saved.expires);
        } catch (SQLException e) {
            System.err.println("Could not validate remembered account: " + e.getMessage());
            return null;
        }
    }

    public void logout(Session s) {
        if (s != null) {
            sessions.revoke(s.token);
        }
    }

    /**
     * Returns account identity and time metadata for a user. Returns
     * {@code null} when the account is unknown. Never exposes the password hash.
     */
    public AccountInfo accountInfo(String username) {
        try {
            return users.accountInfo(username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
        } catch (SQLException e) {
            System.err.println("Could not read account info: " + e.getMessage());
            return null;
        }
    }

    /** db.properties (working dir, then ~/.quizfin), overridable by env vars. */
    private static Properties loadDbSettings() {
        Properties p = new Properties();
        Path custom = Paths.get(System.getProperty("quizfin.db.properties", "db.properties"));
        Path userHome = Paths.get(System.getProperty("user.home"), ".quizfin", "db.properties");
        for (Path candidate : new Path[] { custom, userHome }) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try (InputStream in = Files.newInputStream(candidate)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Could not read " + candidate + ": " + e);
            }
            break;
        }
        override(p, "db.url", "QUIZFIN_DB_URL");
        override(p, "db.user", "QUIZFIN_DB_USER");
        override(p, "db.password", "QUIZFIN_DB_PASSWORD");
        return p;
    }

    private static void override(Properties p, String key, String envKey) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            p.setProperty(key, env.trim());
        }
    }
}
