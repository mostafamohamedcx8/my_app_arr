package com.myapp.aqdeeha.modules.auth.repository;

import com.myapp.aqdeeha.modules.auth.entity.AuthToken;
import com.myapp.aqdeeha.security.AuthPrincipal.PrincipalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update AuthToken t set t.revokedAt = :now
            where t.principalType = :type and t.principalId = :id and t.revokedAt is null
            """)
    int revokeAll(@Param("type") PrincipalType type, @Param("id") Long id, @Param("now") Instant now);
}
