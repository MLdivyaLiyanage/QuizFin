package com.perisic.fish.auth;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A user account's identity and time metadata, safe to hand to the GUI.
 *
 * <p>{@code createdAt} and {@code lastLogin} are the {@code created_at} and
 * {@code last_login} columns of the {@code users} table, shown to the player
 * as a date and time.</p>
 */
public final class AccountInfo {
    public final String username;
    public final String displayName;
    public final Instant createdAt;
    public final Instant lastLogin;

    /** 09 Nov 2026, 09:21 pm */
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a").withZone(ZoneId.systemDefault());

    AccountInfo(String username, String displayName, Instant createdAt, Instant lastLogin) {
        this.username = username;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    /** When the account was created, e.g. {@code 09 Nov 2026, 07:50:12 pm}. */
    public String createdAtDisplay() {
        return createdAt == null ? "—" : FORMAT.format(createdAt);
    }

    /** When the account last logged in, e.g. {@code 09 Nov 2026, 09:21:44 pm}. */
    public String lastLoginDisplay() {
        return lastLogin == null ? "never" : FORMAT.format(lastLogin);
    }

    @Override
    public String toString() {
        return username + " (" + displayName + ")";
    }
}
