package com.myapp.aqdeeha.modules.courier.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.storage.StorageProperties;
import com.myapp.aqdeeha.security.SecretHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Short-lived signed links for ID-card images, so the dashboard can use them in {@code <img src>} without
 * an Authorization header. A link is bound to: courier, side, the admin who requested it, and an expiry
 * (default 5 min). Signature = HMAC-SHA256 with the server pepper; any change to the URL invalidates it.
 */
@Component
@RequiredArgsConstructor
public class IdCardUrlSigner {

    public static final String PATH = "/api/files/id-cards/%d/%s?a=%d&exp=%d&sig=%s";

    private final SecretHasher hasher;
    private final StorageProperties storageProperties;
    private final Clock clock;

    public SignedIdCardUrls sign(Long courierId, Long adminId) {
        Instant expiresAt = clock.instant().plus(storageProperties.signedUrlTtl());
        long exp = expiresAt.getEpochSecond();
        return new SignedIdCardUrls(url(courierId, "front", adminId, exp), url(courierId, "back", adminId, exp),
                expiresAt);
    }

    /** Throws SIGNED_URL_INVALID when the signature is wrong or the link expired. */
    public void verify(Long courierId, String side, Long adminId, long exp, String sig) {
        boolean fresh = clock.instant().getEpochSecond() <= exp;
        if (!fresh || !hasher.matches(payload(courierId, side, adminId, exp), sig)) {
            throw new BusinessException(ErrorCode.SIGNED_URL_INVALID);
        }
    }

    private String url(Long courierId, String side, Long adminId, long exp) {
        return PATH.formatted(courierId, side, adminId, exp, hasher.hash(payload(courierId, side, adminId, exp)));
    }

    private static String payload(Long courierId, String side, Long adminId, long exp) {
        return "id-card|" + courierId + "|" + side + "|" + adminId + "|" + exp;
    }

    public record SignedIdCardUrls(String front, String back, Instant expiresAt) {
    }
}
