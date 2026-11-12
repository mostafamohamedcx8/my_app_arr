package com.myapp.aqdeeha.modules.catalog.entity;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** Home-screen category. Icon is a predefined {@code iconKey} and/or an uploaded image ({@code iconUrl}). */
@Entity
@SQLRestriction("deleted_at is null")
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(name = "icon_key", length = 60)
    private String iconKey;

    /** Storage key of an uploaded icon image ({@code public/...}). */
    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Soft delete: once set, the row is invisible to every query (see @SQLRestriction). */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static Category create(String name, String color, int sortOrder, boolean active, Long createdBy) {
        Category c = new Category();
        c.name = name;
        c.color = color;
        c.sortOrder = sortOrder;
        c.active = active;
        c.createdBy = createdBy;
        return c;
    }
}
