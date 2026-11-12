package com.myapp.aqdeeha.modules.auth.entity;

import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One row per OTP sent. Lifecycle:
 * <pre>
 * sent (code_hash set) --correct code--> verified (code_hash cleared, verification_token_hash set)
 *                                           --verification token used--> consumed
 * </pre>
 * Only hashes are stored; the raw code / verification token exist only in the WhatsApp message / API response.
 */
@Entity
@Table(name = "otp_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "code_hash", length = 64)
    private String codeHash;

    @Convert(converter = OtpPurpose.DbConverter.class)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Convert(converter = OtpSubjectType.DbConverter.class)
    @Column(name = "subject_type", nullable = false, length = 20)
    private OtpSubjectType subjectType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    /** Signup only: terms version the user accepted at step 1 (becomes a terms_acceptances row at step 3). */
    @Column(name = "terms_version_id")
    private Long termsVersionId;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_token_hash", length = 64, unique = true)
    private String verificationTokenHash;

    @Column(name = "verification_expires_at")
    private Instant verificationExpiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static OtpCode issue(String phone, OtpPurpose purpose, OtpSubjectType subjectType, String codeHash,
                                Instant now, Instant expiresAt, String requestIp) {
        OtpCode o = new OtpCode();
        o.phone = phone;
        o.purpose = purpose;
        o.subjectType = subjectType;
        o.codeHash = codeHash;
        o.createdAt = now;
        o.expiresAt = expiresAt;
        o.requestIp = requestIp;
        o.attempts = 0;
        return o;
    }

    public void attachTerms(Long termsVersionId, String userAgent) {
        this.termsVersionId = termsVersionId;
        this.userAgent = userAgent == null || userAgent.length() <= 255 ? userAgent : userAgent.substring(0, 255);
    }

    public void registerFailedAttempt() {
        attempts++;
    }

    public void markVerified(Instant now, String verificationTokenHash, Instant verificationExpiresAt) {
        this.verifiedAt = now;
        this.codeHash = null;
        this.verificationTokenHash = verificationTokenHash;
        this.verificationExpiresAt = verificationExpiresAt;
    }

    public void markConsumed(Instant now) {
        this.consumedAt = now;
    }

    public enum OtpPurpose {
        SIGNUP, RESET_PASSWORD, COURIER_FIX;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<OtpPurpose> {
            public DbConverter() {
                super(OtpPurpose.class);
            }
        }
    }

    public enum OtpSubjectType {
        CUSTOMER, COURIER, ADMIN;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<OtpSubjectType> {
            public DbConverter() {
                super(OtpSubjectType.class);
            }
        }
    }
}
