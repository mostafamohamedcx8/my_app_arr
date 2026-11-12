package com.myapp.aqdeeha.security;

import java.util.Optional;

/**
 * Resolves a raw bearer token to a principal: valid signature, not expired, not revoked,
 * and the account is still allowed in. Implemented by the auth module.
 */
public interface AccessTokenAuthenticator {

    Optional<AuthPrincipal> authenticate(String rawToken);
}
