package com.myapp.aqdeeha.security.access;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Secure-by-default guard: fails application startup if any {@code /api/**} endpoint has none of
 * {@link Protect}, {@link AllowTo} or {@link PublicEndpoint}. Forgetting an annotation can never
 * silently publish an endpoint.
 */
@Component
public class AccessAnnotationVerifier implements SmartInitializingSingleton {

    private final RequestMappingHandlerMapping handlerMapping;

    public AccessAnnotationVerifier(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> unprotected = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : handlerMapping.getHandlerMethods().entrySet()) {
            Set<String> patterns = e.getKey().getPatternValues();
            boolean isApi = patterns.stream().anyMatch(p -> p.startsWith("/api/"));
            if (isApi && !hasAccessAnnotation(e.getValue())) {
                unprotected.add(patterns + " -> " + e.getValue().getShortLogMessage());
            }
        }
        if (!unprotected.isEmpty()) {
            throw new IllegalStateException("Endpoints without @Protect / @AllowTo / @PublicEndpoint: " + unprotected);
        }
    }

    private static boolean hasAccessAnnotation(HandlerMethod hm) {
        return AnnotatedElementUtils.hasAnnotation(hm.getMethod(), Protect.class)
                || AnnotatedElementUtils.hasAnnotation(hm.getMethod(), AllowTo.class)
                || AnnotatedElementUtils.hasAnnotation(hm.getMethod(), PublicEndpoint.class)
                || AnnotatedElementUtils.hasAnnotation(hm.getBeanType(), Protect.class)
                || AnnotatedElementUtils.hasAnnotation(hm.getBeanType(), AllowTo.class);
    }
}
