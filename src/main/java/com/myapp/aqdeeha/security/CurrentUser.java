package com.myapp.aqdeeha.security;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * The one way to get the logged-in account anywhere in the code
 * (controllers, services): {@code CurrentUser.id()}, {@code CurrentUser.get().role()}.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** The current principal, or empty when the request has no valid token. */
    public static Optional<AuthPrincipal> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthPrincipal p ? Optional.of(p) : Optional.empty();
    }

    /** The current principal; throws {@code TOKEN_INVALID} when there is none. */
    public static AuthPrincipal get() {
        return find().orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
    }

    public static Long id() {
        return get().id();
    }
}
