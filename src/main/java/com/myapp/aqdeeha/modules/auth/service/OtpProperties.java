package com.myapp.aqdeeha.modules.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param length             digits in a code (6)
 * @param ttl                code lifetime (5 min)
 * @param maxAttempts        wrong guesses allowed per code (5)
 * @param resendCooldown     min time between two codes to the same phone+purpose (60 s)
 * @param maxPerPhonePerHour codes per phone per rolling hour (5)
 * @param maxPerIpPerHour    codes per client IP per rolling hour
 * @param verificationTtl    lifetime of the token returned after a correct code (to finish signup / reset)
 */
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(int length, Duration ttl, int maxAttempts, Duration resendCooldown,
                            int maxPerPhonePerHour, int maxPerIpPerHour, Duration verificationTtl) {
}
