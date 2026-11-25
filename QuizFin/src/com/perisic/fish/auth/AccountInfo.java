package com.perisic.fish.auth;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A user account's identity and time metadata, safe to hand to the GUI.
 *
 * createdAt and lastLogin are the created_at} and
 * last_login columns of users table, shown to the player
 * as a date and time.
 */
public final class AccountInfo {
    public final String username;
    public final String displayName;
    public final Instant createdAt;
    public final Instant lastLogin;

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a").withZone(ZoneId.systemDefault());

    AccountInfo(String username, String displayName, Instant createdAt, Instant lastLogin) {
        this.username = username;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    /** When account was created. */
    public String createdAtDisplay() {
        return createdAt == null ? "—" : FORMAT.format(createdAt);
    }

    /** When account last logged in. */
    public String lastLoginDisplay() {
        return lastLogin == null ? "never" : FORMAT.format(lastLogin);
    }

    @Override
    public String toString() {
        return username + " (" + displayName + ")";
    }
}
