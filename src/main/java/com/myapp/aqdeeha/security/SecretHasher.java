package com.myapp.aqdeeha.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Hashing and random generation for short-lived secrets (OTP codes, verification tokens, issued JWTs).
 * Uses HMAC-SHA256 with a server-side pepper, so a leaked DB alone cannot brute-force 6-digit codes.
 * Passwords are NOT hashed here — they use BCrypt via {@code PasswordEncoder}.
 */
@Component
public class SecretHasher {

    private final SecretKeySpec pepper;
    private final SecureRandom random = new SecureRandom();

    public SecretHasher(SecurityProperties properties) {
        byte[] key = properties.hashPepper().getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalStateException("app.security.hash-pepper must be at least 32 characters");
        }
        this.pepper = new SecretKeySpec(key, "HmacSHA256");
    }

    /** 64-char lower-case hex HMAC of {@code value}. */
    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(pepper);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Constant-time comparison of {@code raw} against a stored hash. */
    public boolean matches(String raw, String storedHash) {
        if (raw == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(raw).getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    /** Uniformly random numeric code, e.g. 6 digits. */
    public String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /** URL-safe random token with 256 bits of entropy. */
    public String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Constant-time string equality (for configured secrets such as the bootstrap key). */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
