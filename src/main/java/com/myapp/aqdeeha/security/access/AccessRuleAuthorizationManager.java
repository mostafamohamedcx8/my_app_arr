package com.myapp.aqdeeha.security.access;

import com.myapp.aqdeeha.security.AuthPrincipal;
import com.myapp.aqdeeha.security.AuthPrincipal.Role;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The single place where {@link Protect} and {@link AllowTo} are enforced.
 * Resolution: annotation on the method wins over the annotation on the class.
 */
public class AccessRuleAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private static final AuthorizationDecision GRANTED = new AuthorizationDecision(true);
    private static final AuthorizationDecision DENIED = new AuthorizationDecision(false);

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, MethodInvocation mi) {
        Authentication auth = authentication.get();
        AuthPrincipal principal = auth != null && auth.getPrincipal() instanceof AuthPrincipal p ? p : null;
        if (principal == null) {
            return DENIED; // no / invalid token -> mapped to TOKEN_INVALID by GlobalExceptionHandler
        }

        Class<?> targetClass = mi.getThis() != null ? AopUtils.getTargetClass(mi.getThis()) : mi.getMethod().getDeclaringClass();
        Method method = AopUtils.getMostSpecificMethod(mi.getMethod(), targetClass);

        AllowTo allowTo = resolve(method, targetClass);
        if (allowTo == null) {
            // only @Protect applies: any fully active account. A limited COURIER_APPLICANT token
            // reaches only endpoints that list it explicitly in @AllowTo.
            return principal.role() == Role.COURIER_APPLICANT ? DENIED : GRANTED;
        }
        return allowedRoles(allowTo).contains(principal.role()) ? GRANTED : DENIED;
    }

    /** Method-level @AllowTo / @Protect wins; otherwise the class-level one. Returns null when only @Protect applies. */
    private static AllowTo resolve(Method method, Class<?> targetClass) {
        AllowTo onMethod = find(method, AllowTo.class);
        if (onMethod != null) {
            return onMethod;
        }
        if (find(method, Protect.class) != null) {
            return null;
        }
        return find(targetClass, AllowTo.class);
    }

    private static <A extends java.lang.annotation.Annotation> A find(AnnotatedElement element, Class<A> type) {
        return AnnotatedElementUtils.findMergedAnnotation(element, type);
    }

    static Set<Role> allowedRoles(AllowTo allowTo) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (Role r : allowTo.value()) {
            roles.add(r);
        }
        if (roles.contains(Role.ADMIN)) {
            roles.add(Role.SUPER_ADMIN);
        }
        return roles;
    }
}
