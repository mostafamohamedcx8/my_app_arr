package com.myapp.aqdeeha.modules.auth.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.util.PhoneNumbers;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.AuthResponse;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.UserProfileResponse;
import com.myapp.aqdeeha.modules.auth.mapper.AuthMapper;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.courier.service.CourierService;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserRole;
import com.myapp.aqdeeha.modules.user.service.UserService;
import com.myapp.aqdeeha.security.JwtService.IssuedJwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/** Phone + password login for customers and couriers, and the current-user profile. */
@Service
public class MobileAuthService {

    private final UserService userService;
    private final CourierService courierService;
    private final AuthTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper mapper;
    private final Clock clock;
    /** Compared against when the phone is unknown, so response time doesn't reveal registered numbers. */
    private final String dummyHash;

    public MobileAuthService(UserService userService, CourierService courierService, AuthTokenService tokenService,
                             PasswordEncoder passwordEncoder, AuthMapper mapper, Clock clock) {
        this.userService = userService;
        this.courierService = courierService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    @Transactional
    public AuthResponse login(String rawPhone, String password) {
        Optional<User> found = PhoneNumbers.tryNormalize(rawPhone).flatMap(userService::findByPhone);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        CourierProfile courier = null;
        if (user.getRole() == UserRole.COURIER) {
            courier = courierService.findProfile(user.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COURIER_PENDING_APPROVAL));
            switch (courier.getApprovalStatus()) {
                case PENDING -> throw new BusinessException(ErrorCode.COURIER_PENDING_APPROVAL);
                case NEEDS_FIX -> {
                    // Limited login: the token resolves to COURIER_APPLICANT (see AuthTokenService).
                    IssuedJwt jwt = tokenService.issueForUser(user);
                    userService.recordLogin(user.getId(), clock.instant());
                    return mapper.toAuthResponse(jwt, user, courier);
                }
                case REJECTED -> throw new BusinessException(ErrorCode.COURIER_REJECTED);
                case APPROVED -> { }
            }
        }
        switch (user.getStatus()) {
            case SUSPENDED -> throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
            case BANNED -> throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
            case PENDING -> throw new BusinessException(ErrorCode.COURIER_PENDING_APPROVAL);
            case ACTIVE -> { }
        }

        userService.recordLogin(user.getId(), clock.instant());
        IssuedJwt jwt = tokenService.issueForUser(user);
        return mapper.toAuthResponse(jwt, user, courier);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse me(Long userId) {
        User user = userService.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        CourierProfile courier = user.getRole() == UserRole.COURIER
                ? courierService.findProfile(userId).orElse(null)
                : null;
        return mapper.toProfile(user, courier);
    }
}
