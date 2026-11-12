package com.myapp.aqdeeha.modules.user.entity;

import com.myapp.aqdeeha.modules.user.entity.UserEnums.Gender;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserRole;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserStatus;
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
import java.time.LocalDate;

/** Customers and couriers (couriers additionally have a {@code courier_profiles} row). */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = UserRole.DbConverter.class)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "phone_verified_at", nullable = false)
    private Instant phoneVerifiedAt;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Convert(converter = Gender.DbConverter.class)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Convert(converter = UserStatus.DbConverter.class)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(nullable = false)
    private int points;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static User create(UserRole role, String phone, String passwordHash, String name, LocalDate birthDate,
                              Instant phoneVerifiedAt, Instant termsAcceptedAt, UserStatus status) {
        User u = new User();
        u.role = role;
        u.phone = phone;
        u.passwordHash = passwordHash;
        u.name = name;
        u.birthDate = birthDate;
        u.phoneVerifiedAt = phoneVerifiedAt;
        u.termsAcceptedAt = termsAcceptedAt;
        u.status = status;
        u.points = 0;
        return u;
    }
}
