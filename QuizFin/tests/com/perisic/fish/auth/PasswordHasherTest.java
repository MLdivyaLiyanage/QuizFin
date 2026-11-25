package com.perisic.fish.auth;

import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Run with java -cp "target/bcrypt-test;lib/*" com.perisic.fish.auth.PasswordHasherTest. */
public final class PasswordHasherTest {
    public static void main(String[] args) throws Exception {
        char[] password = "sample-password".toCharArray();
        String first = PasswordHasher.hash(password);
        String second = PasswordHasher.hash(password);
        check(first.startsWith("$2a$12$") && first.length() == 60, "bcrypt format and cost");
        check(!first.equals(second), "random salts");
        check(PasswordHasher.verify(password, first), "correct password");
        check(!PasswordHasher.verify("wrong".toCharArray(), first), "wrong password");
        check(!PasswordHasher.verify(null, first), "null password");
        check(!PasswordHasher.verify(password, null), "null hash");
        check(!PasswordHasher.verify(password, "$2a$broken"), "malformed bcrypt");
        check(!PasswordHasher.verify(password, "broken"), "malformed legacy hash");
        char[] limit = "a".repeat(72).toCharArray();
        check(PasswordHasher.verify(limit, PasswordHasher.hash(limit)), "72 byte boundary");
        check(!PasswordHasher.fitsBcrypt("é".repeat(37).toCharArray()), "UTF-8 byte limit");
        check(!PasswordHasher.verify("a".repeat(73).toCharArray(), PasswordHasher.hash(limit)),
                "reject truncated password match");
        try {
            PasswordHasher.hash("a".repeat(73).toCharArray());
            throw new AssertionError("Oversized password was accepted");
        } catch (IllegalArgumentException expected) {
            // Bcrypt must never silently truncate newly registered passwords.
        }
        byte[] salt = new byte[16];
        PBEKeySpec spec = new PBEKeySpec(password, salt, 120_000, 256);
        byte[] digest;
        try {
            digest = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
        String legacy = "120000$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(digest);
        check(PasswordHasher.verify(password, legacy), "legacy account login");
        check(!PasswordHasher.verify("wrong".toCharArray(), legacy), "legacy wrong password");
        check(PasswordHasher.needsRehash(legacy), "legacy migration detection");
        check(!PasswordHasher.needsRehash(first), "bcrypt requires no migration");
        System.out.println("PasswordHasher checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
