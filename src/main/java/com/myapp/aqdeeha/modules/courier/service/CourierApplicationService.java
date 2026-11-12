package com.myapp.aqdeeha.modules.courier.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.exception.FieldErrorsException;
import com.myapp.aqdeeha.common.i18n.Messages;
import com.myapp.aqdeeha.common.storage.FileStorageService;
import com.myapp.aqdeeha.common.storage.ImageValidator;
import com.myapp.aqdeeha.common.storage.ImageValidator.ImageType;
import com.myapp.aqdeeha.common.util.AfterCommit;
import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import com.myapp.aqdeeha.common.util.PlateNumbers;
import com.myapp.aqdeeha.modules.auth.service.AuthTokenService;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierMeResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.FixApplicationResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.FixFieldInfo;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.ResubmitForm;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.ResubmitResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.WalletRequest;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.ApprovalStatus;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.VehicleType;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.WalletType;
import com.myapp.aqdeeha.modules.courier.entity.FixField;
import com.myapp.aqdeeha.modules.courier.mapper.CourierMapper;
import com.myapp.aqdeeha.modules.courier.repository.CourierProfileRepository;
import com.myapp.aqdeeha.modules.courier.repository.CourierRow;
import com.myapp.aqdeeha.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a courier does to their own application:
 * <ul>
 *   <li>needs_fix: see ONLY the fields the admin flagged, and resubmit exactly those (all required, others ignored);</li>
 *   <li>approved: change the wallet.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourierApplicationService {

    private static final String REQUIRED = "validation.required";

    private final CourierProfileRepository repository;
    private final CourierMapper mapper;
    private final ImageValidator imageValidator;
    private final FileStorageService storage;
    private final AuthTokenService tokenService;
    private final Messages messages;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FixApplicationResponse application(Long courierId) {
        CourierRow row = load(courierId);
        CourierProfile p = row.profile();
        List<FixFieldInfo> fields = p.fixFieldSet().stream().map(f -> info(f, row)).toList();
        return new FixApplicationResponse(LowercaseEnumConverter.value(p.getApprovalStatus()), p.getReviewNote(),
                p.getResubmissionCount(), fields);
    }

    /**
     * Applies the flagged fields only, puts the application back to {@code pending} and ends the limited session.
     * Replaced files are deleted only after the transaction commits.
     */
    @Transactional
    public ResubmitResponse resubmit(Long courierId, ResubmitForm f) {
        CourierRow row = load(courierId);
        CourierProfile p = row.profile();
        User u = row.user();
        Set<FixField> required = p.fixFieldSet();
        if (p.getApprovalStatus() != ApprovalStatus.NEEDS_FIX || required.isEmpty()) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_EDITABLE);
        }

        Map<String, String> missing = new LinkedHashMap<>();
        for (FixField field : required) {
            for (String formKey : field.formKeys()) {
                if (!provided(f, formKey)) {
                    missing.put(formKey, REQUIRED);
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new FieldErrorsException(missing);
        }

        // validate everything before touching storage
        ImageType photo = required.contains(FixField.PHOTO) ? imageValidator.validate(f.getPhoto(), "photo", true) : null;
        ImageType front = required.contains(FixField.ID_CARD_FRONT) ? imageValidator.validate(f.getIdCardFront(), "idCardFront", true) : null;
        ImageType back = required.contains(FixField.ID_CARD_BACK) ? imageValidator.validate(f.getIdCardBack(), "idCardBack", true) : null;
        ImageType vehicle = required.contains(FixField.VEHICLE_PHOTO) ? imageValidator.validate(f.getVehiclePhoto(), "vehiclePhoto", true) : null;
        WalletType walletType = null;
        String walletNumber = null;
        if (required.contains(FixField.WALLET)) {
            walletType = WalletNumbers.type(f.getWalletType());
            walletNumber = WalletNumbers.normalize(walletType, f.getWalletNumber());
        }

        if (required.contains(FixField.FULL_NAME)) {
            u.setName(f.getFullName().trim());
        }
        if (required.contains(FixField.BIRTH_DATE)) {
            u.setBirthDate(f.getBirthDate());
        }
        if (required.contains(FixField.ADDRESS)) {
            p.setAddress(f.getAddress().trim());
        }
        if (required.contains(FixField.VEHICLE_TYPE)) {
            p.setVehicleType(LowercaseEnumConverter.parse(VehicleType.class, f.getVehicleType()));
        }
        if (required.contains(FixField.VEHICLE_COLOR)) {
            p.setVehicleColor(f.getVehicleColor().trim());
        }
        if (required.contains(FixField.PLATE_NUMBER)) {
            p.setPlateNumber(PlateNumbers.normalize(f.getPlateNumber()));
        }
        if (walletType != null) {
            p.setWalletType(walletType);
            p.setWalletNumber(walletNumber);
        }

        List<String> newFiles = new ArrayList<>();
        List<String> replacedFiles = new ArrayList<>();
        try {
            if (photo != null) {
                replacedFiles.add(u.getPhotoUrl());
                u.setPhotoUrl(track(newFiles, storage.storePublic(f.getPhoto(), photo, "users")));
            }
            if (vehicle != null) {
                replacedFiles.add(p.getVehiclePhotoUrl());
                p.setVehiclePhotoUrl(track(newFiles, storage.storePublic(f.getVehiclePhoto(), vehicle, "vehicles")));
            }
            if (front != null) {
                replacedFiles.add(p.getIdFrontUrl());
                p.setIdFrontUrl(track(newFiles, storage.storeEncrypted(f.getIdCardFront(), front, "id-cards")));
            }
            if (back != null) {
                replacedFiles.add(p.getIdBackUrl());
                p.setIdBackUrl(track(newFiles, storage.storeEncrypted(f.getIdCardBack(), back, "id-cards")));
            }

            p.setApprovalStatus(ApprovalStatus.PENDING);
            p.setFixFields(null);
            p.setResubmittedAt(clock.instant());
            p.setResubmissionCount(p.getResubmissionCount() + 1);
            repository.flush();
            tokenService.revokeAllForUser(courierId);
        } catch (RuntimeException e) {
            storage.deleteQuietly(newFiles);
            throw e;
        }
        AfterCommit.run(() -> storage.deleteQuietly(replacedFiles.stream().filter(k -> k != null).toList()));
        log.info("Courier {} resubmitted application (#{}) fixing {}", courierId, p.getResubmissionCount(), required);

        return new ResubmitResponse(LowercaseEnumConverter.value(p.getApprovalStatus()), p.getResubmissionCount(),
                messages.get("message.courier_resubmitted"));
    }

    /** Approved courier changes the wallet customers transfer to. */
    @Transactional
    public CourierMeResponse setWallet(Long courierId, WalletRequest req) {
        CourierRow row = load(courierId);
        WalletType type = WalletNumbers.type(req.walletType());
        row.profile().setWalletType(type);
        row.profile().setWalletNumber(WalletNumbers.normalize(type, req.walletNumber()));
        return mapper.toMe(row);
    }

    // ------------------------------------------------------------------ helpers

    private FixFieldInfo info(FixField f, CourierRow row) {
        User u = row.user();
        CourierProfile p = row.profile();
        List<String> options = switch (f) {
            case VEHICLE_TYPE -> Arrays.stream(VehicleType.values()).map(LowercaseEnumConverter::value).toList();
            case WALLET -> Arrays.stream(WalletType.values()).map(LowercaseEnumConverter::value).toList();
            default -> List.of();
        };
        Object current = switch (f) {
            case FULL_NAME -> u.getName();
            case BIRTH_DATE -> u.getBirthDate() == null ? null : u.getBirthDate().toString();
            case PHOTO -> storage.publicUrl(u.getPhotoUrl());
            case ADDRESS -> p.getAddress();
            case VEHICLE_TYPE -> LowercaseEnumConverter.value(p.getVehicleType());
            case VEHICLE_COLOR -> p.getVehicleColor();
            case PLATE_NUMBER -> p.getPlateNumber();
            case VEHICLE_PHOTO -> storage.publicUrl(p.getVehiclePhotoUrl());
            case ID_CARD_FRONT, ID_CARD_BACK -> null; // ID images are never returned
            case WALLET -> {
                Map<String, String> w = new LinkedHashMap<>();
                w.put("walletType", LowercaseEnumConverter.value(p.getWalletType()));
                w.put("walletNumber", p.getWalletNumber());
                yield w;
            }
        };
        return new FixFieldInfo(f.key(), messages.get("field." + f.key()), f.inputType(), f.formKeys(), options,
                current);
    }

    private static boolean provided(ResubmitForm f, String formKey) {
        return switch (formKey) {
            case "fullName" -> notBlank(f.getFullName());
            case "birthDate" -> f.getBirthDate() != null;
            case "address" -> notBlank(f.getAddress());
            case "vehicleType" -> notBlank(f.getVehicleType());
            case "vehicleColor" -> notBlank(f.getVehicleColor());
            case "plateNumber" -> notBlank(f.getPlateNumber());
            case "walletType" -> notBlank(f.getWalletType());
            case "walletNumber" -> notBlank(f.getWalletNumber());
            case "photo" -> hasFile(f.getPhoto());
            case "idCardFront" -> hasFile(f.getIdCardFront());
            case "idCardBack" -> hasFile(f.getIdCardBack());
            case "vehiclePhoto" -> hasFile(f.getVehiclePhoto());
            default -> false;
        };
    }

    private CourierRow load(Long courierId) {
        return repository.findRow(courierId).orElseThrow(() -> new BusinessException(ErrorCode.COURIER_NOT_FOUND));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static String track(List<String> files, String key) {
        files.add(key);
        return key;
    }
}
