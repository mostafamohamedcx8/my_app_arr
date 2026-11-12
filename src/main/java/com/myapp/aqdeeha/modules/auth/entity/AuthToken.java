package com.myapp.aqdeeha.modules.auth.entity;

import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import com.myapp.aqdeeha.security.AuthPrincipal.PrincipalType;
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
 * Every issued JWT is recorded here by its hash only. A token is accepted only while its row
 * exists, is not revoked and not expired — this is what makes JWTs revocable.
 */
@Entity
@Table(name = "auth_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Convert(converter = PrincipalTypeConverter.class)
    @Column(name = "principal_type", nullable = false, length = 10)
    private PrincipalType principalType;

    @Column(name = "principal_id", nullable = false)
    private Long principalId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static AuthToken of(String tokenHash, PrincipalType type, Long principalId, Instant now, Instant expiresAt) {
        AuthToken t = new AuthToken();
        t.tokenHash = tokenHash;
        t.principalType = type;
        t.principalId = principalId;
        t.createdAt = now;
        t.expiresAt = expiresAt;
        return t;
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    @jakarta.persistence.Converter
    public static class PrincipalTypeConverter extends LowercaseEnumConverter<PrincipalType> {
        public PrincipalTypeConverter() {
            super(PrincipalType.class);
        }
    }
}
