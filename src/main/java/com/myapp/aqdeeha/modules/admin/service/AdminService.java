package com.myapp.aqdeeha.modules.admin.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.modules.admin.entity.Admin;
import com.myapp.aqdeeha.modules.admin.entity.Admin.AdminRole;
import com.myapp.aqdeeha.modules.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Public API of the admin module. */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    /**
     * Creates the one and only super admin. The existence check gives a friendly error; the DB unique
     * column {@code super_admin_guard} guarantees uniqueness even under concurrent requests.
     */
    @Transactional
    public Admin createSuperAdmin(String name, String email, String rawPassword) {
        if (adminRepository.existsByRole(AdminRole.SUPER_ADMIN)) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_ALREADY_EXISTS);
        }
        return insert(Admin.create(normalizeEmail(email), passwordEncoder.encode(rawPassword), name.trim(),
                AdminRole.SUPER_ADMIN, null), true);
    }

    /** Creates a regular admin. Caller must already be authorized as super admin. */
    @Transactional
    public Admin createAdmin(String name, String email, String rawPassword, Long createdBy) {
        return insert(Admin.create(normalizeEmail(email), passwordEncoder.encode(rawPassword), name.trim(),
                AdminRole.ADMIN, createdBy), false);
    }

    @Transactional
    public void recordLogin(Long adminId, Instant now) {
        adminRepository.findById(adminId).ifPresent(a -> a.setLastLoginAt(now));
    }

    private Admin insert(Admin admin, boolean superAdmin) {
        if (adminRepository.existsByEmail(admin.getEmail())) {
            throw BusinessException.forField(ErrorCode.EMAIL_ALREADY_REGISTERED, "email");
        }
        try {
            return adminRepository.saveAndFlush(admin);
        } catch (DataIntegrityViolationException e) {
            throw superAdmin
                    ? new BusinessException(ErrorCode.SUPER_ADMIN_ALREADY_EXISTS)
                    : BusinessException.forField(ErrorCode.EMAIL_ALREADY_REGISTERED, "email");
        }
    }
}
