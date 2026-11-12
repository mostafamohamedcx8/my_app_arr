package com.myapp.aqdeeha.modules.auth.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpPurpose;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpSubjectType;
import com.myapp.aqdeeha.modules.auth.repository.OtpCodeRepository;
import com.myapp.aqdeeha.modules.notification.service.WhatsAppSender;
import com.myapp.aqdeeha.security.SecretHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Sends, verifies and consumes WhatsApp OTPs. All phones passed in must already be normalized. */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository repository;
    private final OtpProperties props;
    private final SecretHasher hasher;
    private final WhatsAppSender whatsApp;
    private final Clock clock;

    public record OtpSent(long expiresInSeconds, long resendAfterSeconds) {
    }

    public record Verified(String token, long expiresInSeconds) {
    }

    /** Info the API may always return, even when no code was actually sent. */
    public OtpSent genericSentInfo() {
        return new OtpSent(props.ttl().toSeconds(), props.resendCooldown().toSeconds());
    }

    @Transactional
    public OtpSent send(String phone, OtpPurpose purpose, OtpSubjectType subject, String ip) {
        return send(phone, purpose, subject, ip, null, null);
    }

    /** Signup variant: also stores the terms version accepted at step 1 and the client's user agent. */
    @Transactional
    public OtpSent send(String phone, OtpPurpose purpose, OtpSubjectType subject, String ip, Long termsVersionId,
                        String userAgent) {
        Instant now = clock.instant();

        repository.findFirstByPhoneAndPurposeOrderByIdDesc(phone, purpose).ifPresent(last -> {
            long waited = Duration.between(last.getCreatedAt(), now).toSeconds();
            long cooldown = props.resendCooldown().toSeconds();
            if (waited < cooldown) {
                throw new BusinessException(ErrorCode.OTP_RESEND_TOO_SOON, cooldown - waited);
            }
        });
        Instant hourAgo = now.minus(Duration.ofHours(1));
        if (repository.countByPhoneAndCreatedAtAfter(phone, hourAgo) >= props.maxPerPhonePerHour()) {
            throw new BusinessException(ErrorCode.OTP_HOURLY_LIMIT);
        }
        if (ip != null && repository.countByRequestIpAndCreatedAtAfter(ip, hourAgo) >= props.maxPerIpPerHour()) {
            throw new BusinessException(ErrorCode.OTP_HOURLY_LIMIT);
        }

        String code = hasher.randomDigits(props.length());
        OtpCode otp = OtpCode.issue(phone, purpose, subject, hasher.hash(code), now, now.plus(props.ttl()), ip);
        otp.attachTerms(termsVersionId, userAgent);
        repository.save(otp);
        whatsApp.sendOtp(phone, code, LowercaseEnumConverter.value(purpose));
        return genericSentInfo();
    }

    /**
     * Checks the latest code for phone+purpose. On success returns a one-time verification token.
     * Failed attempts are committed even though an exception is thrown (noRollbackFor).
     *
     * @param expectedSubject required subject type, or null to accept any
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public Verified verify(String phone, OtpPurpose purpose, OtpSubjectType expectedSubject, String code) {
        Instant now = clock.instant();
        OtpCode otp = repository.findTopByPhoneAndPurposeOrderByIdDesc(phone, purpose)
                .filter(o -> o.getCodeHash() != null && o.getConsumedAt() == null)
                .filter(o -> expectedSubject == null || o.getSubjectType() == expectedSubject)
                .orElseThrow(() -> BusinessException.forField(ErrorCode.OTP_INVALID, "code"));

        if (otp.getAttempts() >= props.maxAttempts()) {
            throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }
        if (!now.isBefore(otp.getExpiresAt())) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (!hasher.matches(code, otp.getCodeHash())) {
            otp.registerFailedAttempt();
            if (otp.getAttempts() >= props.maxAttempts()) {
                throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
            }
            throw BusinessException.forField(ErrorCode.OTP_INVALID, "code");
        }

        String token = hasher.randomToken();
        otp.markVerified(now, hasher.hash(token), now.plus(props.verificationTtl()));
        return new Verified(token, props.verificationTtl().toSeconds());
    }

    /**
     * Uses up a verification token (one time). Runs inside the caller's transaction, so if the
     * caller fails later the token stays usable.
     */
    @Transactional
    public OtpCode consume(String verificationToken, OtpPurpose purpose, OtpSubjectType expectedSubject) {
        Instant now = clock.instant();
        OtpCode otp = repository.findByVerificationTokenHash(hasher.hash(verificationToken))
                .filter(o -> o.getPurpose() == purpose)
                .filter(o -> expectedSubject == null || o.getSubjectType() == expectedSubject)
                .filter(o -> o.getConsumedAt() == null)
                .filter(o -> o.getVerificationExpiresAt() != null && now.isBefore(o.getVerificationExpiresAt()))
                .orElseThrow(() -> BusinessException.forField(ErrorCode.VERIFICATION_TOKEN_INVALID,
                        "verificationToken"));
        otp.markConsumed(now);
        return otp;
    }
}
