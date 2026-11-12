package com.myapp.aqdeeha.security.access;

import com.myapp.aqdeeha.security.AuthPrincipal.Role;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The endpoint is allowed only for the listed roles, e.g. {@code @AllowTo(ADMIN)} or
 * {@code @AllowTo({CUSTOMER, COURIER})}. {@code SUPER_ADMIN} is always allowed wherever {@code ADMIN} is.
 * Put it on a controller method or class; a method-level annotation overrides the class one.
 * <p>No valid token → 401 {@code TOKEN_INVALID}; valid token with another role → 403 {@code FORBIDDEN}.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowTo {

    Role[] value();
}
