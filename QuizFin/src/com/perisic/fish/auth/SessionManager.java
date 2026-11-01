package com.perisic.fish.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Properties;

/**
 * Cookie-style sessions for the desktop app.
 * <ul>
 * <li>"Server side": sessions.properties keeps SHA-256(token) -> user|expiry,
 * so a stolen file does not reveal valid tokens.</li>
 * <li>"Client side": session.cookie holds the raw token (like a browser
 * cookie) so "Remember me" can log the player in again until it expires.</li>
 * </ul>
 */
class SessionManager {
    static final long TTL_MS = 7L * 24 * 60 * 60 * 1000; // 7 days, like a persistent cookie

    private static final SecureRandom RANDOM = new SecureRandom();
    private final Path table;
    private final Path cookie;

    SessionManager(Path dir) {
        this.table = dir.resolve("sessions.properties");
        this.cookie = dir.resolve("session.cookie");
    }

    /** Creates a session; when remember is true the cookie file is written. */
    synchronized Session issue(String username, boolean remember) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        long expires = System.currentTimeMillis() + TTL_MS;

        Properties t = load();
        purgeExpired(t);
        t.setProperty(sha(token), username + "|" + expires);
        save(t);

        if (remember) {
            writeCookie(token, username, expires);
        } else {
            deleteCookie();
        }
        return new Session(username, token, expires);
    }

    /** Reads the cookie and returns the session if it is still valid. */
    synchronized Session resume() {
        try {
            if (!Files.exists(cookie)) {
                return null;
            }
            String[] lines = Files.readString(cookie, StandardCharsets.UTF_8).split("\n");
            String token = lines[0].trim();
            String entry = load().getProperty(sha(token));
            if (entry == null) {
                deleteCookie();
                return null;
            }
            int bar = entry.lastIndexOf('|');
            long expires = Long.parseLong(entry.substring(bar + 1));
            if (expires < System.currentTimeMillis()) {
                revoke(token);
                return null;
            }
            return new Session(entry.substring(0, bar), token, expires);
        } catch (IOException | RuntimeException e) {
            deleteCookie();
            return null;
        }
    }

    /** Logout: forget the token on both sides. */
    synchronized void revoke(String token) {
        if (token != null) {
            Properties t = load();
            t.remove(sha(token));
            save(t);
        }
        deleteCookie();
    }

    // ----------------------------------------------------------- helpers

    private void writeCookie(String token, String user, long expires) {
        try {
            Files.writeString(cookie, token + "\n" + user + "\n" + expires, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not write session cookie: " + e);
        }
    }

    private void deleteCookie() {
        try {
            Files.deleteIfExists(cookie);
        } catch (IOException ignored) {
            // nothing more we can do
        }
    }

    private void purgeExpired(Properties t) {
        long now = System.currentTimeMillis();
        t.entrySet().removeIf(e -> {
            String v = String.valueOf(e.getValue());
            try {
                return Long.parseLong(v.substring(v.lastIndexOf('|') + 1)) < now;
            } catch (RuntimeException ex) {
                return true;
            }
        });
    }

    private Properties load() {
        Properties p = new Properties();
        if (Files.exists(table)) {
            try (InputStream in = Files.newInputStream(table)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Could not read sessions: " + e);
            }
        }
        return p;
    }

    private void save(Properties p) {
        try (OutputStream out = Files.newOutputStream(table)) {
            p.store(out, "QuizFin sessions (token hashes only)");
        } catch (IOException e) {
            System.err.println("Could not save sessions: " + e);
        }
    }

    private static String sha(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
