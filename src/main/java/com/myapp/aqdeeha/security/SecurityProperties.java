package com.myapp.aqdeeha.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param jwt          JWT signing settings
 * @param hashPepper   server-side secret mixed into OTP / token hashes (HMAC-SHA256)
 * @param bootstrapKey secret required by the one-time super-admin bootstrap endpoint
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Jwt jwt, String hashPepper, String bootstrapKey) {

    /**
     * @param secret base64 HMAC key, at least 32 bytes
     * @param issuer {@code iss} claim
     * @param ttl    token lifetime (30 days)
     */
    public record Jwt(String secret, String issuer, Duration ttl) {
    }
}
