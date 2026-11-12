package com.myapp.aqdeeha.modules.auth.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.modules.admin.dto.AdminResponse;
import com.myapp.aqdeeha.modules.admin.dto.CreateAdminRequest;
import com.myapp.aqdeeha.modules.admin.entity.Admin;
import com.myapp.aqdeeha.modules.admin.service.AdminService;
import com.myapp.aqdeeha.modules.auth.dto.AuthResponses.AdminAuthResponse;
import com.myapp.aqdeeha.modules.auth.mapper.AuthMapper;
import com.myapp.aqdeeha.security.SecretHasher;
import com.myapp.aqdeeha.security.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/** Email + password login for admins, and the one-time super-admin bootstrap. */
@Slf4j
@Service
public class AdminAuthService {

    private final AdminService adminService;
    private final AuthTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper mapper;
    private final SecurityProperties securityProperties;
    private final Clock clock;
    private final String dummyHash;

    public AdminAuthService(AdminService adminService, AuthTokenService tokenService, PasswordEncoder passwordEncoder,
                            AuthMapper mapper, SecurityProperties securityProperties, Clock clock) {
        this.adminService = adminService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.securityProperties = securityProperties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    @Transactional
    public AdminAuthResponse login(String email, String password) {
        Optional<Admin> found = adminService.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            throw new BusinessException(ErrorCode.ADMIN_INVALID_CREDENTIALS);
        }
        Admin admin = found.get();
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_CREDENTIALS);
        }
        if (!admin.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        adminService.recordLogin(admin.getId(), clock.instant());
        return mapper.toAdminAuthResponse(tokenService.issueForAdmin(admin), admin);
    }

    /**
     * Works only while no super admin exists AND the caller knows {@code app.security.bootstrap-key}.
     * After the first success it fails forever with SUPER_ADMIN_ALREADY_EXISTS.
     */
    @Transactional
    public AdminAuthResponse bootstrapSuperAdmin(String bootstrapKey, CreateAdminRequest request) {
        String configured = securityProperties.bootstrapKey();
        if (configured == null || configured.isBlank() || !SecretHasher.constantTimeEquals(configured, bootstrapKey)) {
            throw new BusinessException(ErrorCode.BOOTSTRAP_KEY_INVALID);
        }
        Admin admin = adminService.createSuperAdmin(request.name(), request.email(), request.password());
        log.warn("Super admin bootstrapped: id={} email={}", admin.getId(), admin.getEmail());
        return mapper.toAdminAuthResponse(tokenService.issueForAdmin(admin), admin);
    }

    @Transactional(readOnly = true)
    public AdminResponse me(Long adminId) {
        return adminService.findById(adminId).map(AdminResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
    }
}
