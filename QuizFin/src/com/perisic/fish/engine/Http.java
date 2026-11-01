package com.perisic.fish.engine;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.List;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Small HTTP helper shared by the two API clients (timeouts + status codes).
 */
final class Http {

    /** IOException that carries the HTTP status code. */
    static class HttpException extends IOException {
        private static final long serialVersionUID = 1L;
        final int code;

        HttpException(int code, String url) {
            super("HTTP " + code + " from " + url);
            this.code = code;
        }
    }

    /**
     * Cookie jar shared by all API calls: cookies set by Open Trivia DB / Fish
     * API are stored and sent back automatically by HttpURLConnection.
     */
    private static final CookieManager COOKIES = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    static {
        CookieHandler.setDefault(COOKIES);
    }

    private Http() {
    }

    /** Cookies currently held for API servers (for diagnostics / reports). */
    static List<HttpCookie> cookies() {
        return COOKIES.getCookieStore().getCookies();
    }

    static String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent", "QuizFin/1.0");
        int code = c.getResponseCode();
        if (code != 200) {
            throw new HttpException(code, url);
        }
        try (InputStream in = c.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }
}
