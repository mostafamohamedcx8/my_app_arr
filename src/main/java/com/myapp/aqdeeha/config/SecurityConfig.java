package com.myapp.aqdeeha.config;

import com.myapp.aqdeeha.security.AccessTokenAuthenticator;
import com.myapp.aqdeeha.security.JsonSecurityErrorHandler;
import com.myapp.aqdeeha.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * HTTP layer only <b>authenticates</b> (reads the bearer token). <b>Authorization</b> is declared per endpoint
 * with {@code @Protect} / {@code @AllowTo} / {@code @PublicEndpoint} and enforced by method security
 * ({@link MethodSecurityConfig}). {@code AccessAnnotationVerifier} refuses to start the app if an
 * {@code /api} endpoint has none of them, so nothing becomes public by accident.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * CORS only matters for browsers (the Expo web build / admin dashboard); the native apps ignore it.
     * {@code app.cors.allowed-origins}: comma-separated, empty = no cross-origin browser access.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:}") String origins) {
        List<String> allowed = Arrays.stream(origins.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList();
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowed);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Bootstrap-Key"));
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http, AccessTokenAuthenticator authenticator,
                                        JsonSecurityErrorHandler errors) throws Exception {
        base(http, authenticator, errors)
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultChain(HttpSecurity http, AccessTokenAuthenticator authenticator,
                                            JsonSecurityErrorHandler errors) throws Exception {
        base(http, authenticator, errors)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/files/public/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    private HttpSecurity base(HttpSecurity http, AccessTokenAuthenticator authenticator,
                              JsonSecurityErrorHandler errors) {
        return http
                .cors(c -> { })
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .addFilterBefore(new JwtAuthenticationFilter(authenticator), UsernamePasswordAuthenticationFilter.class);
    }
}
