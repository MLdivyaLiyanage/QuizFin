package com.perisic.fish.auth;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SessionManagerTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("target", "test-sessions");
        Files.createDirectories(root);
        Path dir = Files.createTempDirectory(root, "session-");
        try {
            SessionManager manager = new SessionManager(dir);
            check(manager.resume() == null, "No cookie means no session");
            Session remembered = manager.issue("player", true);
            check(remembered.token.length() >= 43, "256-bit random token");
            check(manager.resume().username.equals("player"), "Remembered account resumes");
            check(!Files.readString(dir.resolve("sessions.properties")).contains(remembered.token),
                    "Session table stores a token hash, not raw token");
            manager.revoke(remembered.token);
            check(manager.resume() == null, "Logout revokes session");
            manager.issue("player", false);
            check(!Files.exists(dir.resolve("session.cookie")), "Unchecked remember does not persist cookie");
            manager.issue("player", true);
            Properties table = new Properties();
            try (InputStream in = Files.newInputStream(dir.resolve("sessions.properties"))) { table.load(in); }
            for (String key : table.stringPropertyNames()) table.setProperty(key, "player|0");
            try (OutputStream out = Files.newOutputStream(dir.resolve("sessions.properties"))) { table.store(out, "expired"); }
            check(manager.resume() == null, "Expired token rejected");
            Files.writeString(dir.resolve("session.cookie"), "forged-token");
            check(manager.resume() == null, "Unknown token rejected");
            System.out.println("SessionManagerTest passed: remember, logout, expiry, unknown token, hashed storage.");
        } finally {
            Files.deleteIfExists(dir.resolve("session.cookie"));
            Files.deleteIfExists(dir.resolve("sessions.properties"));
            Files.deleteIfExists(dir);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
