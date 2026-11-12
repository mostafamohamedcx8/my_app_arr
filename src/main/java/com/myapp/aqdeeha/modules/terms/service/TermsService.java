package com.myapp.aqdeeha.modules.terms.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.modules.audit.service.AuditService;
import com.myapp.aqdeeha.modules.terms.dto.TermsDtos.AdminTermsResponse;
import com.myapp.aqdeeha.modules.terms.dto.TermsDtos.TermsResponse;
import com.myapp.aqdeeha.modules.terms.dto.TermsDtos.TermsStatusResponse;
import com.myapp.aqdeeha.modules.terms.entity.TermsAcceptance;
import com.myapp.aqdeeha.modules.terms.entity.TermsVersion;
import com.myapp.aqdeeha.modules.terms.entity.TermsVersion.Audience;
import com.myapp.aqdeeha.modules.terms.repository.TermsAcceptanceRepository;
import com.myapp.aqdeeha.modules.terms.repository.TermsVersionRepository;
import com.myapp.aqdeeha.security.AuthPrincipal.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Versioned terms & conditions per audience, and the append-only record of who accepted which version.
 * <ul>
 *   <li>Signup step 1 must send {@code acceptTerms=true} + the current {@code termsVersion}
 *       ({@link #requireCurrentAccepted}); the account's acceptance row is written at step 3.</li>
 *   <li>{@link #isUpToDate} feeds {@code termsUpToDate} in login / me responses.</li>
 *   <li>The super admin publishes a new version ({@link #publish}); every user then has to accept it again.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TermsService {

    public static final String ACTION_TERMS_PUBLISHED = "terms.published";
    private static final int MAX_USER_AGENT = 255;

    private final TermsVersionRepository versions;
    private final TermsAcceptanceRepository acceptances;
    private final AuditService auditService;
    private final Clock clock;

    // ---------- audience helpers ----------

    /** "customer" / "COURIER" → Audience; anything else → INVALID_TERMS_AUDIENCE. */
    public static Audience parseAudience(String raw) {
        try {
            return Audience.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.forField(ErrorCode.INVALID_TERMS_AUDIENCE, "audience");
        }
    }

    /** Which terms a mobile account has to accept. A needs_fix courier (COURIER_APPLICANT) is still a courier. */
    public static Audience audienceOf(Role role) {
        return switch (role) {
            case CUSTOMER -> Audience.CUSTOMER;
            case COURIER, COURIER_APPLICANT -> Audience.COURIER;
            default -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };
    }

    // ---------- apps ----------

    @Transactional(readOnly = true)
    public TermsResponse current(String audience) {
        return toResponse(requireCurrent(parseAudience(audience)));
    }

    /**
     * Signup step 1: the user ticked the box for exactly the version that is current now.
     *
     * @return the accepted (current) version, to remember on the OTP row
     */
    @Transactional(readOnly = true)
    public TermsVersion requireCurrentAccepted(Audience audience, Boolean acceptTerms, Integer termsVersion) {
        TermsVersion current = requireCurrent(audience);
        if (!Boolean.TRUE.equals(acceptTerms) || termsVersion == null || termsVersion != current.getVersion()) {
            throw BusinessException.forField(ErrorCode.TERMS_NOT_ACCEPTED, "acceptTerms");
        }
        return current;
    }

    /** Signup step 3: writes the legal record for the version accepted at step 1 (same transaction as the account). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSignupAcceptance(Long userId, Long termsVersionId, Instant acceptedAt, String ip, String userAgent) {
        if (termsVersionId == null) {
            // OTP sent before terms versioning existed → the user never accepted a version
            throw BusinessException.forField(ErrorCode.TERMS_NOT_ACCEPTED, "acceptTerms");
        }
        acceptances.save(TermsAcceptance.of(userId, termsVersionId, acceptedAt, ip, clip(userAgent)));
    }

    @Transactional(readOnly = true)
    public boolean isUpToDate(Long userId, Audience audience) {
        return versions.findByAudienceAndCurrentTrue(audience)
                .map(current -> acceptances.existsByUserIdAndTermsVersionId(userId, current.getId()))
                .orElse(true); // no terms published → nothing to accept
    }

    /** POST /api/v1/terms/accept — only the current version of the user's own audience. Idempotent. */
    @Transactional
    public TermsStatusResponse accept(Long userId, Role role, Long termsVersionId, String ip, String userAgent) {
        TermsVersion current = requireCurrent(audienceOf(role));
        if (!current.getId().equals(termsVersionId)) {
            throw BusinessException.forField(ErrorCode.TERMS_VERSION_INVALID, "termsVersionId");
        }
        if (!acceptances.existsByUserIdAndTermsVersionId(userId, current.getId())) {
            acceptances.save(TermsAcceptance.of(userId, current.getId(), clock.instant(), ip, clip(userAgent)));
        }
        return new TermsStatusResponse(true, current.getId(), current.getVersion());
    }

    // ---------- admin ----------

    /** Super admin publishes a new version (number = last + 1) and makes it current. Audited. */
    @Transactional
    public AdminTermsResponse publish(String audienceRaw, String content, Long adminId) {
        Audience audience = parseAudience(audienceRaw);
        Integer previousVersion = versions.lockCurrent(audience).map(old -> {
            old.retire();
            return old.getVersion();
        }).orElse(null);
        versions.flush(); // free the "one current" guard before inserting the new current row

        int next = versions.maxVersion(audience) + 1;
        TermsVersion created = versions.saveAndFlush(
                TermsVersion.publish(audience, next, content.strip(), clock.instant(), adminId));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("audience", audience.name());
        details.put("version", next);
        details.put("previousVersion", previousVersion);
        auditService.record(adminId, ACTION_TERMS_PUBLISHED, AuditService.TARGET_TERMS, created.getId(), details);
        return toAdmin(created, 0);
    }

    @Transactional(readOnly = true)
    public List<AdminTermsResponse> history(String audience) {
        return versions.findByAudienceOrderByVersionDesc(parseAudience(audience)).stream()
                .map(t -> toAdmin(t, acceptances.countByTermsVersionId(t.getId())))
                .toList();
    }

    // ---------- internals ----------

    private TermsVersion requireCurrent(Audience audience) {
        return versions.findByAudienceAndCurrentTrue(audience)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));
    }

    private static String clip(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        return userAgent.length() <= MAX_USER_AGENT ? userAgent : userAgent.substring(0, MAX_USER_AGENT);
    }

    private static TermsResponse toResponse(TermsVersion t) {
        return new TermsResponse(t.getId(), t.getAudience().name(), t.getVersion(), t.getContent(), t.getPublishedAt());
    }

    private static AdminTermsResponse toAdmin(TermsVersion t, long acceptanceCount) {
        return new AdminTermsResponse(t.getId(), t.getAudience().name(), t.getVersion(), t.getContent(),
                t.getPublishedAt(), t.isCurrent(), t.getCreatedBy(), acceptanceCount);
    }
}
