package com.myapp.aqdeeha.security.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The endpoint requires a valid token — any fully active account (customer, courier, admin, super admin).
 * A limited {@code COURIER_APPLICANT} token (courier in needs_fix) is NOT accepted here.
 * Put it on a controller method or on the controller class. A method-level annotation overrides the class one.
 * <p>Missing / invalid / revoked token → 401 {@code TOKEN_INVALID}.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Protect {
}
