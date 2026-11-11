package com.perisic.fish.auth;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Bcrypt password hashing with a fresh random salt and cost 12.
 * Legacy PBKDF2 hashes remain readable so existing accounts can be upgraded.
 */
final class PasswordHasher {
    private static final int BCRYPT_COST = 12;
    private static final int KEY_BITS = 256;

    private PasswordHasher() {
    }

    static String hash(char[] password) {
        if (!fitsBcrypt(password)) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes.");
        }
        return BCrypt.hashpw(new String(password), BCrypt.gensalt(BCRYPT_COST));
    }

    static boolean fitsBcrypt(char[] password) {
        return password != null && new String(password).getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    static boolean needsRehash(String stored) {
        return stored != null && !stored.startsWith("$2a$");
    }

    static boolean verify(char[] password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        try {
            if (stored.startsWith("$2a$")) {
                return fitsBcrypt(password) && BCrypt.checkpw(new String(password), stored);
            }
            String[] p = stored.split("\\$");
            if (p.length != 3 || !"120000".equals(p[0])) {
                return false;
            }
            int iterations = Integer.parseInt(p[0]);
            byte[] salt = Base64.getDecoder().decode(p[1]);
            byte[] expected = Base64.getDecoder().decode(p[2]);
            // constant-time comparison
            return MessageDigest.isEqual(expected, pbkdf2(password, salt, iterations));
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Used only to verify passwords saved before the bcrypt migration.
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }
}
