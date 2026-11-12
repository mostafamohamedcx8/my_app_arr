package com.myapp.aqdeeha.modules.auth.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.util.PhoneNumbers;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.OtpVerifiedResponse;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpPurpose;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpSubjectType;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Forgot password for customers and couriers: phone → OTP → verify → new password.
 * Step 1 fails with PHONE_NOT_REGISTERED for unknown phones.
 * A successful reset revokes every token the user had.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final OtpService otpService;
    private final UserService userService;
    private final AuthTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    /** Fails with PHONE_NOT_REGISTERED when no customer/courier has this phone; otherwise sends the OTP. */
    public OtpService.OtpSent forgot(String rawPhone, String ip) {
        String phone = PhoneNumbers.normalize(rawPhone);
        User user = userService.findByPhone(phone)
                .orElseThrow(() -> BusinessException.forField(ErrorCode.PHONE_NOT_REGISTERED, "phone"));
        OtpSubjectType subject = OtpSubjectType.valueOf(user.getRole().name());
        return otpService.send(phone, OtpPurpose.RESET_PASSWORD, subject, ip);
    }

    public OtpVerifiedResponse verify(String rawPhone, String code) {
        OtpService.Verified v = otpService.verify(PhoneNumbers.normalize(rawPhone), OtpPurpose.RESET_PASSWORD,
                null, code);
        return new OtpVerifiedResponse(v.token(), v.expiresInSeconds());
    }

    @Transactional
    public void reset(String resetToken, String newPassword) {
        OtpCode otp = otpService.consume(resetToken, OtpPurpose.RESET_PASSWORD, null);
        User user = userService.findByPhone(otp.getPhone())
                .orElseThrow(() -> BusinessException.forField(ErrorCode.VERIFICATION_TOKEN_INVALID, "resetToken"));
        userService.changePassword(user.getId(), passwordEncoder.encode(newPassword), clock.instant());
        int revoked = tokenService.revokeAllForUser(user.getId());
        log.info("Password reset for user {} — {} token(s) revoked", user.getId(), revoked);
    }
}
