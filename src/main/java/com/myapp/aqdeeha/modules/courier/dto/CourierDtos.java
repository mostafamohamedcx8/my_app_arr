package com.myapp.aqdeeha.modules.courier.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.myapp.aqdeeha.common.util.PlateNumbers;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response bodies of the courier endpoints. */
public final class CourierDtos {

    private CourierDtos() {
    }

    // ------------------------------------------------------------------ requests

    /**
     * PATCH /api/admin/couriers/{id}/approval. {@code note} is required unless approving.
     * {@code fields} is required for needs_fix: which parts the courier must correct
     * (fullName, birthDate, photo, address, vehicleType, vehicleColor, plateNumber, vehiclePhoto,
     * idCardFront, idCardBack, wallet). Ignored for other decisions.
     */
    public record ApprovalRequest(
            @NotBlank(message = "validation.required")
            @Pattern(regexp = "^(approved|rejected|needs_fix)$", message = "validation.approval_status")
            String status,
            @Size(max = 500, message = "validation.note_size")
            String note,
            List<String> fields) {
    }

    /**
     * PUT /api/v1/courier/wallet. vodafone_cash → Egyptian mobile number;
     * instapay → Egyptian mobile number or InstaPay address (name@instapay).
     */
    public record WalletRequest(
            @NotBlank(message = "validation.required")
            @Pattern(regexp = "^(vodafone_cash|instapay)$", message = "validation.wallet_type")
            String walletType,
            @NotBlank(message = "validation.required")
            @Size(max = 100, message = "validation.wallet_number")
            String walletNumber) {
    }

    /**
     * POST /api/v1/courier/application/resubmit (multipart). Send exactly the fields the admin asked to fix
     * (see GET /api/v1/courier/application → requiredFields[].formKeys). All of them are required; anything
     * else is ignored. Images: jpg / png / webp, max 5 MB.
     */
    @Getter
    @Setter
    public static class ResubmitForm {
        @Size(min = 2, max = 100, message = "validation.name_size")
        private String fullName;

        @Past(message = "validation.birth_date_past")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate birthDate;

        @Size(min = 2, max = 255, message = "validation.address_size")
        private String address;

        @Pattern(regexp = "^(car|scooter|motorcycle|bicycle)$", message = "validation.vehicle_type")
        private String vehicleType;

        @Size(min = 1, max = 30, message = "validation.vehicle_color_size")
        private String vehicleColor;

        @Size(max = 20, message = "validation.plate_number_size")
        @Pattern(regexp = PlateNumbers.REGEX, message = PlateNumbers.MESSAGE)
        private String plateNumber;

        @Pattern(regexp = "^(vodafone_cash|instapay)$", message = "validation.wallet_type")
        private String walletType;

        @Size(max = 100, message = "validation.wallet_number")
        private String walletNumber;

        private MultipartFile photo;
        private MultipartFile idCardFront;
        private MultipartFile idCardBack;
        private MultipartFile vehiclePhoto;
    }

    // ------------------------------------------------------------------ responses

    public record ResubmitResponse(String approvalStatus, int resubmissionCount, String message) {
    }

    /** GET /api/v1/courier/application — only what the admin asked to fix. */
    public record FixApplicationResponse(String approvalStatus, String reviewNote, int resubmissionCount,
                                         List<FixFieldInfo> requiredFields) {
    }

    /**
     * One field to fix. {@code inputType}: text | date | select | image | wallet.
     * {@code formKeys}: multipart keys to send on resubmit. {@code options}: allowed values (select / wallet type).
     * {@code currentValue}: what is on file now (text value or public image URL; never ID-card images).
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record FixFieldInfo(String field, String label, String inputType, List<String> formKeys,
                               List<String> options, Object currentValue) {
    }

    /** One row of the admin courier list. No ID-card images. */
    public record CourierSummaryResponse(Long id, String name, String phone, String photoUrl, String vehicleType,
                                         String approvalStatus, String accountStatus, String reviewNote,
                                         int resubmissionCount, Instant resubmittedAt, boolean hasWallet,
                                         Instant createdAt) {
    }

    /** One admin decision on the application (from audit_log). */
    public record DecisionHistoryItem(String from, String to, String note, List<String> fields, Long adminId,
                                      String adminName, Instant at) {
    }

    /**
     * Full courier record for admins.
     * <ul>
     *   <li>{@code idCardFrontUrl}/{@code idCardBackUrl}: signed links valid until {@code idCardUrlsExpireAt}
     *   (5 min) — usable directly in {@code <img src>}, no token needed. Call this endpoint again for fresh links.</li>
     *   <li>{@code idCardFrontDownloadUrl}/{@code idCardBackDownloadUrl}: same image, requires the admin's bearer
     *   token (for fetch()-as-file).</li>
     * </ul>
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CourierAdminDetailResponse(Long id, String name, String phone, String email, LocalDate birthDate,
                                             String gender, String photoUrl, String accountStatus, int points,
                                             Instant createdAt, Instant lastLoginAt,
                                             String address, String vehicleType, String vehicleColor,
                                             String plateNumber, String vehiclePhotoUrl,
                                             String walletType, String walletNumber,
                                             String approvalStatus, String reviewNote, List<String> fixFields,
                                             Long reviewedBy, Instant reviewedAt, int resubmissionCount,
                                             Instant resubmittedAt,
                                             String idCardFrontUrl, String idCardBackUrl, Instant idCardUrlsExpireAt,
                                             String idCardFrontDownloadUrl, String idCardBackDownloadUrl,
                                             List<DecisionHistoryItem> history) {
    }

    /** GET /api/v1/courier/me — everything about the courier except the ID-card images. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CourierMeResponse(Long id, String name, String phone, String email, LocalDate birthDate,
                                    String gender, String photoUrl, String accountStatus, int points,
                                    Instant createdAt, Instant lastLoginAt,
                                    String address, String vehicleType, String vehicleColor, String plateNumber,
                                    String vehiclePhotoUrl, String walletType, String walletNumber,
                                    String approvalStatus, String reviewNote, List<String> fixFields,
                                    int resubmissionCount, String nextStep, boolean termsUpToDate) {
    }
}
