package com.perisic.fish.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Authentication facade used by the GUI: register, login, resume ("remember
 * me" cookie) and logout. Passwords are hashed (PasswordHasher); sessions are
 * handled by SessionManager. Data lives in ~/.quizfin (override with
 * -Dquizfin.dir=...).
 */
public class AuthService {
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 30_000;

    private final Path users;
    private final SessionManager sessions;
    private final Map<String, int[]> fails = new HashMap<>(); // user -> {count}
    private final Map<String, Long> lockedUntil = new HashMap<>();

    public AuthService() {
        this(Paths.get(System.getProperty("quizfin.dir",
                Paths.get(System.getProperty("user.home"), ".quizfin").toString())));
    }

    public AuthService(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Could not create " + dir + ": " + e);
        }
        this.users = dir.resolve("users.properties");
        this.sessions = new SessionManager(dir);
        seedDefaultUser();
    }

    /** Keeps the account from the assignment (Marc / hello26) available. */
    private void seedDefaultUser() {
        Properties p = load();
        if (p.isEmpty()) {
            p.setProperty("marc", "Marc|" + PasswordHasher.hash("hello26".toCharArray()));
            save(p);
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
        Properties p = load();
        if (p.containsKey(name.toLowerCase())) {
            throw new AuthException("That username is already taken.");
        }
        p.setProperty(name.toLowerCase(), name + "|" + PasswordHasher.hash(password));
        save(p);
    }

    /** Checks credentials and starts a session (cookie written if remember). */
    public synchronized Session login(String username, char[] password, boolean remember) throws AuthException {
        String key = username == null ? "" : username.trim().toLowerCase();
        long now = System.currentTimeMillis();
        Long until = lockedUntil.get(key);
        if (until != null && until > now) {
            throw new AuthException("Too many attempts. Try again in " + ((until - now) / 1000 + 1) + " s.");
        }
        String entry = load().getProperty(key);
        boolean ok = false;
        String display = key;
        if (entry != null) {
            int bar = entry.indexOf('|');
            display = entry.substring(0, bar);
            ok = PasswordHasher.verify(password, entry.substring(bar + 1));
        }
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
        return sessions.issue(display, remember);
    }

    /** Auto-login from the "remember me" cookie, or null. */
    public Session resume() {
        return sessions.resume();
    }

    public void logout(Session s) {
        if (s != null) {
            sessions.revoke(s.token);
        }
    }

    private Properties load() {
        Properties p = new Properties();
        if (Files.exists(users)) {
            try (InputStream in = Files.newInputStream(users)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Could not read users: " + e);
            }
        }
        return p;
    }

    private void save(Properties p) {
        try (OutputStream out = Files.newOutputStream(users)) {
            p.store(out, "QuizFin users (salted PBKDF2 hashes)");
        } catch (IOException e) {
            System.err.println("Could not save users: " + e);
        }
    }
}
