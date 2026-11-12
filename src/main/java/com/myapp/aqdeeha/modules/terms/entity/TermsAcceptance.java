package com.myapp.aqdeeha.modules.terms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Legal record that a user accepted a terms version. Append-only: {@link Immutable} in Hibernate and
 * DB triggers that reject UPDATE / DELETE.
 */
@Entity
@Immutable
@Table(name = "terms_acceptances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "terms_version_id", nullable = false)
    private Long termsVersionId;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    public static TermsAcceptance of(Long userId, Long termsVersionId, Instant acceptedAt, String ip, String userAgent) {
        TermsAcceptance a = new TermsAcceptance();
        a.userId = userId;
        a.termsVersionId = termsVersionId;
        a.acceptedAt = acceptedAt;
        a.ipAddress = ip;
        a.userAgent = userAgent;
        return a;
    }
}
