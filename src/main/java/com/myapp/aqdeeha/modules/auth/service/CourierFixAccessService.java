package com.myapp.aqdeeha.modules.auth.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.util.PhoneNumbers;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.AuthResponse;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpPurpose;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpSubjectType;
import com.myapp.aqdeeha.modules.auth.mapper.AuthMapper;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.ApprovalStatus;
import com.myapp.aqdeeha.modules.courier.service.CourierService;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserRole;
import com.myapp.aqdeeha.modules.user.service.UserService;
import com.myapp.aqdeeha.security.JwtService.IssuedJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * A courier asked to fix the application opens it with the phone number + a WhatsApp code (no password).
 * The resulting token is the limited COURIER_APPLICANT one: it can only see the flagged fields and resubmit.
 */
@Service
@RequiredArgsConstructor
public class CourierFixAccessService {

    private final OtpService otpService;
    private final UserService userService;
    private final CourierService courierService;
    private final AuthTokenService tokenService;
    private final AuthMapper mapper;
    private final Clock clock;

    public OtpService.OtpSent sendOtp(String rawPhone, String ip) {
        String phone = PhoneNumbers.normalize(rawPhone);
        requireNeedsFix(phone);
        return otpService.send(phone, OtpPurpose.COURIER_FIX, OtpSubjectType.COURIER, ip);
    }

    /** Correct code → limited token. Wrong-code attempts are counted (OtpService commits them). */
    public AuthResponse verify(String rawPhone, String code) {
        String phone = PhoneNumbers.normalize(rawPhone);
        OtpService.Verified verified = otpService.verify(phone, OtpPurpose.COURIER_FIX, OtpSubjectType.COURIER, code);
        OtpCode otp = otpService.consume(verified.token(), OtpPurpose.COURIER_FIX, OtpSubjectType.COURIER);
        Courier c = requireNeedsFix(otp.getPhone());
        userService.recordLogin(c.user().getId(), clock.instant());
        IssuedJwt jwt = tokenService.issueForUser(c.user());
        return mapper.toAuthResponse(jwt, c.user(), c.profile());
    }

    private Courier requireNeedsFix(String phone) {
        User user = userService.findByPhone(phone)
                .filter(u -> u.getRole() == UserRole.COURIER)
                .orElseThrow(() -> BusinessException.forField(ErrorCode.NOTHING_TO_FIX, "phone"));
        CourierProfile profile = courierService.findProfile(user.getId())
                .filter(p -> p.getApprovalStatus() == ApprovalStatus.NEEDS_FIX)
                .orElseThrow(() -> BusinessException.forField(ErrorCode.NOTHING_TO_FIX, "phone"));
        return new Courier(user, profile);
    }

    private record Courier(User user, CourierProfile profile) {
    }
}
