package com.myapp.aqdeeha.modules.setting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One business value (fee, limit, rate…). Rows are seeded by Flyway; the API only updates existing keys. */
@Entity
@Table(name = "application_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationConfig {

    public enum ValueType { INT, DECIMAL, STRING, BOOLEAN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100, updatable = false)
    private String key;

    @Column(nullable = false, length = 255)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 10, updatable = false)
    private ValueType valueType;

    @Column(length = 255)
    private String description;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void change(String newValue, Long adminId, Instant now) {
        this.value = newValue;
        this.updatedBy = adminId;
        this.updatedAt = now;
    }
}
