package com.myapp.aqdeeha.modules.courier.mapper;

import com.myapp.aqdeeha.common.storage.FileStorageService;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierAdminDetailResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierMeResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierSummaryResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.DecisionHistoryItem;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.ApprovalStatus;
import com.myapp.aqdeeha.modules.courier.repository.CourierRow;
import com.myapp.aqdeeha.modules.courier.service.IdCardUrlSigner.SignedIdCardUrls;
import com.myapp.aqdeeha.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.myapp.aqdeeha.common.util.LowercaseEnumConverter.value;

/** Entity → DTO. ID-card storage keys are never copied into a DTO; only signed / protected URLs are. */
@Component
@RequiredArgsConstructor
public class CourierMapper {

    public static final String ID_CARD_DOWNLOAD_URL = "/api/admin/couriers/%d/id-card/%s";
    public static final String NEXT_STEP_FIX_APPLICATION = "fix_application";
    public static final String NEXT_STEP_ADD_WALLET = "add_wallet";

    private final FileStorageService storage;
    private final com.myapp.aqdeeha.modules.terms.service.TermsService termsService;

    public CourierSummaryResponse toSummary(CourierRow row) {
        User u = row.user();
        CourierProfile p = row.profile();
        return new CourierSummaryResponse(u.getId(), u.getName(), u.getPhone(), storage.publicUrl(u.getPhotoUrl()),
                value(p.getVehicleType()), value(p.getApprovalStatus()), value(u.getStatus()), p.getReviewNote(),
                p.getResubmissionCount(), p.getResubmittedAt(), p.hasWallet(), p.getCreatedAt());
    }

    public CourierAdminDetailResponse toAdminDetail(CourierRow row, SignedIdCardUrls idCards,
                                                    List<DecisionHistoryItem> history) {
        User u = row.user();
        CourierProfile p = row.profile();
        return new CourierAdminDetailResponse(u.getId(), u.getName(), u.getPhone(), u.getEmail(), u.getBirthDate(),
                value(u.getGender()), storage.publicUrl(u.getPhotoUrl()), value(u.getStatus()), u.getPoints(),
                u.getCreatedAt(), u.getLastLoginAt(),
                p.getAddress(), value(p.getVehicleType()), p.getVehicleColor(), p.getPlateNumber(),
                storage.publicUrl(p.getVehiclePhotoUrl()), value(p.getWalletType()), p.getWalletNumber(),
                value(p.getApprovalStatus()), p.getReviewNote(), fixKeys(p), p.getReviewedBy(), p.getReviewedAt(),
                p.getResubmissionCount(), p.getResubmittedAt(),
                idCards.front(), idCards.back(), idCards.expiresAt(),
                ID_CARD_DOWNLOAD_URL.formatted(u.getId(), "front"), ID_CARD_DOWNLOAD_URL.formatted(u.getId(), "back"),
                history);
    }

    public CourierMeResponse toMe(CourierRow row) {
        User u = row.user();
        CourierProfile p = row.profile();
        return new CourierMeResponse(u.getId(), u.getName(), u.getPhone(), u.getEmail(), u.getBirthDate(),
                value(u.getGender()), storage.publicUrl(u.getPhotoUrl()), value(u.getStatus()), u.getPoints(),
                u.getCreatedAt(), u.getLastLoginAt(),
                p.getAddress(), value(p.getVehicleType()), p.getVehicleColor(), p.getPlateNumber(),
                storage.publicUrl(p.getVehiclePhotoUrl()), value(p.getWalletType()), p.getWalletNumber(),
                value(p.getApprovalStatus()), p.getReviewNote(), fixKeys(p), p.getResubmissionCount(), nextStep(p),
                termsService.isUpToDate(u.getId(), com.myapp.aqdeeha.modules.terms.entity.TermsVersion.Audience.COURIER));
    }

    private static List<String> fixKeys(CourierProfile p) {
        return p.fixFieldSet().stream().map(com.myapp.aqdeeha.modules.courier.entity.FixField::key).toList();
    }

    /** Same rule as the login response's {@code nextStep}. */
    public static String nextStep(CourierProfile p) {
        if (p.getApprovalStatus() == ApprovalStatus.NEEDS_FIX) {
            return NEXT_STEP_FIX_APPLICATION;
        }
        if (p.getApprovalStatus() == ApprovalStatus.APPROVED && !p.hasWallet()) {
            return NEXT_STEP_ADD_WALLET;
        }
        return null;
    }
}
