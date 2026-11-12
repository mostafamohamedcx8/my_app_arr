package com.myapp.aqdeeha.config;

import com.myapp.aqdeeha.security.access.AccessRuleAuthorizationManager;
import com.myapp.aqdeeha.security.access.AllowTo;
import com.myapp.aqdeeha.security.access.Protect;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Wires {@link Protect} / {@link AllowTo} into Spring Security method security.
 * Every call to a method (or class) carrying one of them goes through {@link AccessRuleAuthorizationManager}.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = false)
public class MethodSecurityConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor accessRuleAdvisor() {
        Pointcut pointcut = Pointcuts.union(
                Pointcuts.union(new AnnotationMatchingPointcut(Protect.class, true),
                        AnnotationMatchingPointcut.forMethodAnnotation(Protect.class)),
                Pointcuts.union(new AnnotationMatchingPointcut(AllowTo.class, true),
                        AnnotationMatchingPointcut.forMethodAnnotation(AllowTo.class)));
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(pointcut, new AccessRuleAuthorizationManager());
        interceptor.setOrder(AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder() - 1);
        return interceptor;
    }
}
