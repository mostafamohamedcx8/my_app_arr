package com.myapp.aqdeeha.modules.admin.entity;

import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Dashboard accounts. At most one row may have role {@code super_admin}; this is enforced by the
 * DB through the generated unique column {@code super_admin_guard} (not mapped here).
 */
@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Convert(converter = AdminRole.DbConverter.class)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Admin create(String email, String passwordHash, String name, AdminRole role, Long createdBy) {
        Admin a = new Admin();
        a.email = email;
        a.passwordHash = passwordHash;
        a.name = name;
        a.role = role;
        a.active = true;
        a.createdBy = createdBy;
        return a;
    }

    public enum AdminRole {
        ADMIN, SUPER_ADMIN;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<AdminRole> {
            public DbConverter() {
                super(AdminRole.class);
            }
        }
    }
}
