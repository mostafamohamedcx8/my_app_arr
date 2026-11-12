package com.myapp.aqdeeha.modules.auth.service;

import com.myapp.aqdeeha.modules.admin.entity.Admin;
import com.myapp.aqdeeha.modules.admin.service.AdminService;
import com.myapp.aqdeeha.modules.auth.entity.AuthToken;
import com.myapp.aqdeeha.modules.auth.repository.AuthTokenRepository;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.ApprovalStatus;
import com.myapp.aqdeeha.modules.courier.service.CourierService;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserRole;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserStatus;
import com.myapp.aqdeeha.modules.user.service.UserService;
import com.myapp.aqdeeha.security.AccessTokenAuthenticator;
import com.myapp.aqdeeha.security.AuthPrincipal;
import com.myapp.aqdeeha.security.AuthPrincipal.PrincipalType;
import com.myapp.aqdeeha.security.AuthPrincipal.Role;
import com.myapp.aqdeeha.security.JwtService;
import com.myapp.aqdeeha.security.SecretHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/** Issues JWTs (storing only their hash), validates them per request, and revokes them. */
@Service
@RequiredArgsConstructor
public class AuthTokenService implements AccessTokenAuthenticator {

    private final JwtService jwtService;
    private final AuthTokenRepository repository;
    private final SecretHasher hasher;
    private final UserService userService;
    private final AdminService adminService;
    private final CourierService courierService;
    private final Clock clock;

    @Transactional
    public JwtService.IssuedJwt issueForUser(User user) {
        return issue(PrincipalType.USER, user.getId(), Role.valueOf(user.getRole().name()));
    }

    @Transactional
    public JwtService.IssuedJwt issueForAdmin(Admin admin) {
        return issue(PrincipalType.ADMIN, admin.getId(), Role.valueOf(admin.getRole().name()));
    }

    /** Revokes every token of a user (used by password reset). */
    @Transactional
    public int revokeAllForUser(Long userId) {
        return repository.revokeAll(PrincipalType.USER, userId, clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthPrincipal> authenticate(String rawToken) {
        if (jwtService.decode(rawToken).isEmpty()) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        return repository.findByTokenHash(hasher.hash(rawToken))
                .filter(t -> t.isUsable(now))
                .flatMap(this::loadPrincipal);
    }

    private Optional<AuthPrincipal> loadPrincipal(AuthToken token) {
        if (token.getPrincipalType() == PrincipalType.USER) {
            return userService.findById(token.getPrincipalId()).flatMap(this::userPrincipal);
        }
        return adminService.findById(token.getPrincipalId())
                .filter(Admin::isActive)
                .map(a -> new AuthPrincipal(PrincipalType.ADMIN, a.getId(), Role.valueOf(a.getRole().name())));
    }

    /**
     * Active account → its real role. A courier whose application is {@code needs_fix} (account still pending)
     * → {@code COURIER_APPLICANT}, which only reaches the "fix my application" endpoints. Anything else → rejected.
     */
    private Optional<AuthPrincipal> userPrincipal(User u) {
        if (u.getStatus() == UserStatus.ACTIVE) {
            return Optional.of(new AuthPrincipal(PrincipalType.USER, u.getId(), Role.valueOf(u.getRole().name())));
        }
        boolean applicant = u.getRole() == UserRole.COURIER && u.getStatus() == UserStatus.PENDING
                && courierService.findProfile(u.getId())
                .map(p -> p.getApprovalStatus() == ApprovalStatus.NEEDS_FIX).orElse(false);
        return applicant
                ? Optional.of(new AuthPrincipal(PrincipalType.USER, u.getId(), Role.COURIER_APPLICANT))
                : Optional.empty();
    }

    private JwtService.IssuedJwt issue(PrincipalType type, Long id, Role role) {
        String subject = type.name().toLowerCase() + ":" + id;
        JwtService.IssuedJwt jwt = jwtService.issue(subject, role.name());
        repository.save(AuthToken.of(hasher.hash(jwt.value()), type, id, clock.instant(), jwt.expiresAt()));
        return jwt;
    }
}
