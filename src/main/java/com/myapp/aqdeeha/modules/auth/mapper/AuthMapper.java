package com.myapp.aqdeeha.modules.auth.mapper;

import com.myapp.aqdeeha.common.storage.FileStorageService;
import com.myapp.aqdeeha.modules.admin.dto.AdminResponse;
import com.myapp.aqdeeha.modules.admin.entity.Admin;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.AdminAuthResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.AuthResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.CourierInfo;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.UserProfileResponse;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.terms.entity.TermsVersion.Audience;
import com.myapp.aqdeeha.modules.terms.service.TermsService;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserRole;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.security.JwtService.IssuedJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.myapp.aqdeeha.common.util.LowercaseEnumConverter.value;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private static final String BEARER = "Bearer";

    private final FileStorageService storage;
    private final TermsService termsService;

    public AuthResponse toAuthResponse(IssuedJwt jwt, User user, CourierProfile courier) {
        return new AuthResponse(jwt.value(), BEARER, jwt.expiresAt(), nextStep(courier), toProfile(user, courier));
    }

    private static String nextStep(CourierProfile courier) {
        return courier == null ? null : com.myapp.aqdeeha.modules.courier.mapper.CourierMapper.nextStep(courier);
    }

    public AdminAuthResponse toAdminAuthResponse(IssuedJwt jwt, Admin admin) {
        return new AdminAuthResponse(jwt.value(), BEARER, jwt.expiresAt(), AdminResponse.from(admin));
    }

    /** Never includes ID-card images. */
    public UserProfileResponse toProfile(User u, CourierProfile c) {
        CourierInfo courier = c == null ? null : new CourierInfo(c.getAddress(), value(c.getVehicleType()),
                c.getVehicleColor(), c.getPlateNumber(), storage.publicUrl(c.getVehiclePhotoUrl()),
                value(c.getApprovalStatus()), c.getReviewNote(), c.getWalletNumber(), value(c.getWalletType()));
        return new UserProfileResponse(u.getId(), value(u.getRole()), u.getName(), u.getPhone(), u.getEmail(),
                value(u.getGender()), u.getBirthDate(), storage.publicUrl(u.getPhotoUrl()), value(u.getStatus()),
                u.getCreatedAt(), courier, termsService.isUpToDate(u.getId(),
                        u.getRole() == UserRole.COURIER ? Audience.COURIER : Audience.CUSTOMER));
    }
}
