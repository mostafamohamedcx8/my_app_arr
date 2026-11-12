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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * A shop inside a category. {@code opensAt}/{@code closesAt} are Africa/Cairo wall-clock times:
 * closes before opens = open past midnight; equal = open 24h. {@code open} is the admin's manual switch.
 */
@Entity
@SQLRestriction("deleted_at is null")
@Table(name = "shops")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 120)
    private String name;

    /** Storage key ({@code public/shops/...}). */
    @Column(name = "photo_url", nullable = false, length = 255)
    private String photoUrl;

    @Column(nullable = false, length = 255)
    private String street;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;

    @Column(name = "is_open", nullable = false)
    private boolean open;

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

    public static Shop create(Long categoryId, String name, String street, BigDecimal lat, BigDecimal lng,
                              LocalTime opensAt, LocalTime closesAt, boolean open, Long createdBy) {
        Shop s = new Shop();
        s.categoryId = categoryId;
        s.name = name;
        s.street = street;
        s.lat = lat;
        s.lng = lng;
        s.opensAt = opensAt;
        s.closesAt = closesAt;
        s.open = open;
        s.createdBy = createdBy;
        return s;
    }

    /** True when the manual switch is on and {@code now} (Cairo time) falls inside the opening hours. */
    public static boolean isOpenAt(boolean open, LocalTime opensAt, LocalTime closesAt, LocalTime now) {
        if (!open) {
            return false;
        }
        if (opensAt.equals(closesAt)) {
            return true; // 24h
        }
        if (opensAt.isBefore(closesAt)) {
            return !now.isBefore(opensAt) && now.isBefore(closesAt);
        }
        return !now.isBefore(opensAt) || now.isBefore(closesAt); // past midnight, e.g. 18:00 -> 02:00
    }
}
