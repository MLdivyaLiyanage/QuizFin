package com.perisic.fish.auth;

/** A logged-in user's session. The token is what the "cookie" carries. */
public class Session {
    public final String username;
    public final String token;
    public final long expires;

    Session(String username, String token, long expires) {
        this.username = username;
        this.token = token;
        this.expires = expires;
    }

    /** Anonymous session for testing without login. */
    public static Session guest() {
        return new Session("Guest", null, Long.MAX_VALUE);
    }
}
