package com.perisic.fish.auth;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import com.perisic.fish.engine.Leaderboard;
import com.perisic.fish.engine.ScoreEntry;
import com.perisic.fish.engine.ScoreStorageException;

/** Uses an isolated, temporary PostgreSQL schema, never the application's users table.
 * AI-assisted regression test; see SOURCES.md. Run separately from offline tests. */
public final class AuthServiceIntegrationTest {
    public static void main(String[] args) throws Exception {
        Properties settings = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("db.properties"))) { settings.load(in); }
        override(settings, "db.url", "QUIZFIN_DB_URL");
        override(settings, "db.user", "QUIZFIN_DB_USER");
        override(settings, "db.password", "QUIZFIN_DB_PASSWORD");
        String url = settings.getProperty("db.url");
        String schema = "quizfin_test_" + UUID.randomUUID().toString().replace("-", "");
        Path root = Path.of("target", "test-sessions");
        Files.createDirectories(root);
        Path dir = Files.createTempDirectory(root, "auth-");
        Locale originalLocale = Locale.getDefault();
        String originalConfig = System.getProperty("quizfin.db.properties");
        try (Connection connection = DriverManager.getConnection(url, settings.getProperty("db.user"), settings.getProperty("db.password"));
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE SCHEMA " + schema);
            try {
                // PostgreSQL URL property scopes every repository connection to this test schema.
                settings.setProperty("db.url", url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema);
                Path config = dir.resolve("db.properties");
                try (java.io.OutputStream out = Files.newOutputStream(config)) { settings.store(out, "isolated integration database"); }
                System.setProperty("quizfin.db.properties", config.toAbsolutePath().toString());
                AuthService auth = new AuthService(dir, settings);
                try (ResultSet rows = statement.executeQuery("SELECT count(*) FROM " + schema + ".users")) {
                    rows.next();
                    check(rows.getInt(1) == 0, "No built-in account with a hardcoded password");
                }
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                auth.register("IDENTITY", "test-password".toCharArray());
                Session session = auth.login("identity", "test-password".toCharArray(), true);
                check(session.username.equals("identity"), "Canonical ID independent of display name and locale");
                check(auth.resume().username.equals("identity"), "Remembered canonical identity");
                check(auth.accountInfo(session.username) != null, "Session identity resolves to database account");
                try {
                    auth.login("identity", "wrong-password".toCharArray(), false);
                    throw new AssertionError("Wrong password accepted");
                } catch (AuthException expected) { }
                try {
                    auth.register("identity", "test-password".toCharArray());
                    throw new AssertionError("Duplicate account accepted");
                } catch (AuthException expected) { }
                try (ResultSet rows = statement.executeQuery("SELECT password_hash FROM " + schema + ".users")) {
                    rows.next();
                    check(rows.getString(1).startsWith("$2a$12$"), "Database stores salted bcrypt");
                }
                auth.logout(session);
                check(auth.resume() == null, "Logout rejects remember-me session");
                auth.login("identity", "test-password".toCharArray(), true);
                statement.executeUpdate("DELETE FROM " + schema + ".users");
                check(auth.resume() == null, "Deleted account cannot resume");
                // Environment URLs take priority over config files in the production loader.
                // Do not construct a leaderboard if that would escape the isolated schema.
                if (System.getenv("QUIZFIN_DB_URL") == null || System.getenv("QUIZFIN_DB_URL").isBlank()) {
                    Leaderboard scores = new Leaderboard();
                    scores.add(new ScoreEntry("IDENTITY", 20, 30, 1000));
                    scores.add(new ScoreEntry("identity", null, 15, 2000));
                    check(scores.top(10).size() == 2 && scores.top(1).get(0).score == 50, "Real score persistence and ranking");
                    check(scores.best("identity") == 50, "Legacy mixed-case scores belong to canonical account");
                    statement.executeUpdate("DROP TABLE " + schema + ".scores");
                    try {
                        scores.add(new ScoreEntry("identity", null, 15, 3000));
                        throw new AssertionError("Failed score insert reported success");
                    } catch (ScoreStorageException expected) { }
                    try {
                        scores.top(10);
                        throw new AssertionError("Failed score read reported empty leaderboard");
                    } catch (ScoreStorageException expected) { }
                    System.out.println("Leaderboard integration passed: persistence, ranking, identity, visible storage failures.");
                } else {
                    System.out.println("Leaderboard integration skipped because QUIZFIN_DB_URL overrides isolated configuration.");
                }
                System.out.println("AuthServiceIntegrationTest passed: registration, bcrypt, canonical identity, locale, login, resume, logout, deleted account.");
            } finally {
                statement.executeUpdate("DROP SCHEMA " + schema + " CASCADE");
            }
        } finally {
            Locale.setDefault(originalLocale);
            if (originalConfig == null) System.clearProperty("quizfin.db.properties");
            else System.setProperty("quizfin.db.properties", originalConfig);
            Files.deleteIfExists(dir.resolve("db.properties"));
            Files.deleteIfExists(dir.resolve("session.cookie"));
            Files.deleteIfExists(dir.resolve("sessions.properties"));
            Files.deleteIfExists(dir);
        }
    }

    private static void override(Properties settings, String key, String environment) {
        String value = System.getenv(environment);
        if (value != null && !value.isBlank()) settings.setProperty(key, value);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
