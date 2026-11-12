package com.myapp.aqdeeha.modules.terms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One published version of the terms. Text, audience and number never change; only {@code current} moves. */
@Entity
@Table(name = "terms_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsVersion {

    public enum Audience { CUSTOMER, COURIER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private Audience audience;

    @Column(nullable = false, updatable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String content;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    public static TermsVersion publish(Audience audience, int version, String content, Instant now, Long adminId) {
        TermsVersion t = new TermsVersion();
        t.audience = audience;
        t.version = version;
        t.content = content;
        t.publishedAt = now;
        t.current = true;
        t.createdBy = adminId;
        return t;
    }

    public void retire() {
        this.current = false;
    }
}
