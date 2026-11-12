package com.myapp.aqdeeha.security.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicitly marks an endpoint as reachable without a token (login, signup, OTP...).
 * Every {@code /api/**} endpoint must carry {@link Protect}, {@link AllowTo} or this annotation,
 * otherwise the application refuses to start (see {@link AccessAnnotationVerifier}).
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
}
