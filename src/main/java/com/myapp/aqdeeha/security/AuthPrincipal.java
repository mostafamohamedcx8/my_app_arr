package com.myapp.aqdeeha.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The authenticated caller, available in controllers via {@code @AuthenticationPrincipal AuthPrincipal}.
 *
 * @param type users-table account or admins-table account
 * @param id   primary key in that table
 * @param role CUSTOMER, COURIER, ADMIN or SUPER_ADMIN
 */
public record AuthPrincipal(PrincipalType type, Long id, Role role) {

    public enum PrincipalType { USER, ADMIN }

    /**
     * {@code COURIER_APPLICANT}: a courier whose application is {@code needs_fix}. Never stored — derived on each
     * request from the courier's approval status. It can only view its data, resubmit, and read notifications.
     */
    public enum Role { CUSTOMER, COURIER, COURIER_APPLICANT, ADMIN, SUPER_ADMIN }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
