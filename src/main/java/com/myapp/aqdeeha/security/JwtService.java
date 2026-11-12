package com.myapp.aqdeeha.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/** Issues and verifies HS256 JWTs. Revocation is handled separately via the {@code auth_tokens} table. */
@Component
public class JwtService {

    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final SecurityProperties.Jwt props;
    private final Clock clock;

    public JwtService(SecurityProperties properties, Clock clock) {
        this.props = properties.jwt();
        this.clock = clock;
        byte[] keyBytes = Base64.getDecoder().decode(props.secret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret must be base64 of at least 32 bytes");
        }
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        nimbusDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()));
        this.decoder = nimbusDecoder;
    }

    public IssuedJwt issue(String subject, String role) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(props.ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(ROLE_CLAIM, role)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedJwt(value, expiresAt);
    }

    /** Verifies signature, issuer and expiry. Empty when invalid. */
    public Optional<Jwt> decode(String token) {
        try {
            return Optional.of(decoder.decode(token));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public record IssuedJwt(String value, Instant expiresAt) {
    }
}
