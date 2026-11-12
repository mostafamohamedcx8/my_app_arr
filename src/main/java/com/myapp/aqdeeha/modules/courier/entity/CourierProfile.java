package com.myapp.aqdeeha.modules.courier.entity;

import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
 * Courier-only data, 1:1 with {@code users}. ID-card columns hold storage keys of
 * <b>encrypted</b> files and must never be exposed in API responses.
 */
@Entity
@Table(name = "courier_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourierProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 255)
    private String address;

    /** Where customers send transfers. Filled by the courier on first login after approval. */
    @Column(name = "wallet_number", length = 100)
    private String walletNumber;

    @Convert(converter = WalletType.DbConverter.class)
    @Column(name = "wallet_type", length = 20)
    private WalletType walletType;

    @Convert(converter = VehicleType.DbConverter.class)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "vehicle_color", nullable = false, length = 30)
    private String vehicleColor;

    @Column(name = "plate_number", length = 20)
    private String plateNumber;

    @Column(name = "vehicle_photo_url", nullable = false, length = 255)
    private String vehiclePhotoUrl;

    @Column(name = "id_front_url", nullable = false, length = 255)
    private String idFrontUrl;

    @Column(name = "id_back_url", nullable = false, length = 255)
    private String idBackUrl;

    @Convert(converter = ApprovalStatus.DbConverter.class)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    /** needs_fix only: comma-separated {@link FixField} keys the courier must correct. */
    @Column(name = "fix_fields", length = 500)
    private String fixFields;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "resubmitted_at")
    private Instant resubmittedAt;

    @Column(name = "resubmission_count", nullable = false)
    private int resubmissionCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static CourierProfile pending(Long userId) {
        CourierProfile p = new CourierProfile();
        p.userId = userId;
        p.approvalStatus = ApprovalStatus.PENDING;
        return p;
    }

    public enum VehicleType {
        CAR, SCOOTER, MOTORCYCLE, BICYCLE;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<VehicleType> {
            public DbConverter() {
                super(VehicleType.class);
            }
        }
    }

    public java.util.Set<FixField> fixFieldSet() {
        return FixField.parse(fixFields);
    }

    public boolean hasWallet() {
        return walletNumber != null && walletType != null;
    }

    public enum WalletType {
        VODAFONE_CASH, INSTAPAY;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<WalletType> {
            public DbConverter() {
                super(WalletType.class);
            }
        }
    }

    public enum ApprovalStatus {
        PENDING, NEEDS_FIX, APPROVED, REJECTED;

        @jakarta.persistence.Converter
        public static class DbConverter extends LowercaseEnumConverter<ApprovalStatus> {
            public DbConverter() {
                super(ApprovalStatus.class);
            }
        }
    }
}
