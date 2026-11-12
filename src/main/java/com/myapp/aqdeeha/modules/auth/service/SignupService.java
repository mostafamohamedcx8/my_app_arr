package com.myapp.aqdeeha.modules.auth.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.i18n.Messages;
import com.myapp.aqdeeha.common.storage.FileStorageService;
import com.myapp.aqdeeha.common.storage.ImageValidator;
import com.myapp.aqdeeha.common.storage.ImageValidator.ImageType;
import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import com.myapp.aqdeeha.common.util.PlateNumbers;
import com.myapp.aqdeeha.common.util.PhoneNumbers;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.AuthResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.CourierSignupResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.OtpVerifiedResponse;
import com.myapp.aqdeeha.modules.auth.dto.CourierSignupForm;
import com.myapp.aqdeeha.modules.auth.dto.CustomerSignupForm;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpPurpose;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpSubjectType;
import com.myapp.aqdeeha.modules.auth.mapper.AuthMapper;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.VehicleType;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.WalletType;
import com.myapp.aqdeeha.modules.courier.service.CourierService;
import com.myapp.aqdeeha.modules.courier.service.WalletNumbers;
import com.myapp.aqdeeha.modules.terms.entity.TermsVersion;
import com.myapp.aqdeeha.modules.terms.entity.TermsVersion.Audience;
import com.myapp.aqdeeha.modules.terms.service.TermsService;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.Gender;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserRole;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserStatus;
import com.myapp.aqdeeha.modules.user.service.UserService;
import com.myapp.aqdeeha.security.JwtService.IssuedJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 3-step signup for customers and couriers:
 * 1) send OTP  2) verify OTP → verification token  3) complete profile with that token.
 * No {@code users} row exists until step 3 succeeds.
 */
@Service
@RequiredArgsConstructor
public class SignupService {

    private final OtpService otpService;
    private final UserService userService;
    private final CourierService courierService;
    private final AuthTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ImageValidator imageValidator;
    private final FileStorageService storage;
    private final AuthMapper mapper;
    private final TermsService termsService;
    private final Messages messages;
    private final Clock clock;

    /** Step 1: phone + acceptance of the current terms version the app showed. */
    public OtpService.OtpSent sendOtp(String rawPhone, Boolean acceptTerms, Integer termsVersion,
                                      OtpSubjectType subject, String ip, String userAgent) {
        String phone = PhoneNumbers.normalize(rawPhone);
        Audience audience = subject == OtpSubjectType.COURIER ? Audience.COURIER : Audience.CUSTOMER;
        TermsVersion accepted = termsService.requireCurrentAccepted(audience, acceptTerms, termsVersion);
        if (userService.phoneExists(phone)) {
            throw BusinessException.forField(ErrorCode.PHONE_ALREADY_REGISTERED, "phone");
        }
        return otpService.send(phone, OtpPurpose.SIGNUP, subject, ip, accepted.getId(), userAgent);
    }

    public OtpVerifiedResponse verifyOtp(String rawPhone, String code, OtpSubjectType subject) {
        OtpService.Verified v = otpService.verify(PhoneNumbers.normalize(rawPhone), OtpPurpose.SIGNUP, subject, code);
        return new OtpVerifiedResponse(v.token(), v.expiresInSeconds());
    }

    @Transactional
    public AuthResponse completeCustomer(CustomerSignupForm form) {
        ImageType photoType = imageValidator.validate(form.getPhoto(), "photo", false);
        String email = normalizeEmail(form.getEmail());
        OtpCode otp = otpService.consume(form.getVerificationToken(), OtpPurpose.SIGNUP, OtpSubjectType.CUSTOMER);

        List<String> storedFiles = new ArrayList<>();
        try {
            User user = User.create(UserRole.CUSTOMER, otp.getPhone(), passwordEncoder.encode(form.getPassword()),
                    form.getFullName().trim(), form.getBirthDate(), otp.getVerifiedAt(), otp.getCreatedAt(),
                    UserStatus.ACTIVE);
            user.setGender(LowercaseEnumConverter.parse(Gender.class, form.getGender()));
            user.setEmail(email);
            user.setLastLoginAt(clock.instant());
            if (photoType != null) {
                user.setPhotoUrl(store(storedFiles, storage.storePublic(form.getPhoto(), photoType, "users")));
            }
            user = userService.create(user);
            recordTerms(user, otp);
            IssuedJwt jwt = tokenService.issueForUser(user);
            return mapper.toAuthResponse(jwt, user, null);
        } catch (RuntimeException e) {
            storage.deleteQuietly(storedFiles);
            throw e;
        }
    }

    /** Courier account is created as pending; no token is returned until an admin approves it. */
    @Transactional
    public CourierSignupResponse completeCourier(CourierSignupForm form) {
        ImageType photoType = imageValidator.validate(form.getPhoto(), "photo", true);
        ImageType frontType = imageValidator.validate(form.getIdCardFront(), "idCardFront", true);
        ImageType backType = imageValidator.validate(form.getIdCardBack(), "idCardBack", true);
        ImageType vehicleType = imageValidator.validate(form.getVehiclePhoto(), "vehiclePhoto", true);
        WalletType walletType = WalletNumbers.type(form.getWalletType());
        String walletNumber = WalletNumbers.normalize(walletType, form.getWalletNumber());
        OtpCode otp = otpService.consume(form.getVerificationToken(), OtpPurpose.SIGNUP, OtpSubjectType.COURIER);

        List<String> storedFiles = new ArrayList<>();
        try {
            User user = User.create(UserRole.COURIER, otp.getPhone(), passwordEncoder.encode(form.getPassword()),
                    form.getFullName().trim(), form.getBirthDate(), otp.getVerifiedAt(), otp.getCreatedAt(),
                    UserStatus.PENDING);
            user.setPhotoUrl(store(storedFiles, storage.storePublic(form.getPhoto(), photoType, "users")));
            user = userService.create(user);
            recordTerms(user, otp);

            CourierProfile profile = CourierProfile.pending(user.getId());
            profile.setAddress(form.getAddress().trim());
            profile.setVehicleType(LowercaseEnumConverter.parse(VehicleType.class, form.getVehicleType()));
            profile.setVehicleColor(form.getVehicleColor().trim());
            String plate = blankToNull(form.getPlateNumber());
            profile.setPlateNumber(plate == null ? null : PlateNumbers.normalize(plate));
            profile.setWalletType(walletType);
            profile.setWalletNumber(walletNumber);
            profile.setVehiclePhotoUrl(store(storedFiles,
                    storage.storePublic(form.getVehiclePhoto(), vehicleType, "vehicles")));
            profile.setIdFrontUrl(store(storedFiles,
                    storage.storeEncrypted(form.getIdCardFront(), frontType, "id-cards")));
            profile.setIdBackUrl(store(storedFiles,
                    storage.storeEncrypted(form.getIdCardBack(), backType, "id-cards")));
            courierService.createProfile(profile);

            return new CourierSignupResponse(user.getId(), LowercaseEnumConverter.value(user.getStatus()),
                    LowercaseEnumConverter.value(profile.getApprovalStatus()),
                    messages.get("message.courier_signup_pending"));
        } catch (RuntimeException e) {
            storage.deleteQuietly(storedFiles);
            throw e;
        }
    }

    /** Legal record of the terms accepted at step 1 (IP / user agent of that request). */
    private void recordTerms(User user, OtpCode otp) {
        termsService.recordSignupAcceptance(user.getId(), otp.getTermsVersionId(), otp.getCreatedAt(),
                otp.getRequestIp(), otp.getUserAgent());
    }

    private static String store(List<String> stored, String key) {
        stored.add(key);
        return key;
    }

    private static String normalizeEmail(String email) {
        String e = blankToNull(email);
        return e == null ? null : e.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
