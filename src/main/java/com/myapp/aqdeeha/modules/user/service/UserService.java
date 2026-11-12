package com.myapp.aqdeeha.modules.user.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Public API of the user module. Other modules must go through this service, not the repository. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean phoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /** Inserts and flushes immediately so unique-constraint races surface as a clean error. */
    @Transactional
    public User create(User user) {
        if (userRepository.existsByPhone(user.getPhone())) {
            throw BusinessException.forField(ErrorCode.PHONE_ALREADY_REGISTERED, "phone");
        }
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw BusinessException.forField(ErrorCode.EMAIL_ALREADY_REGISTERED, "email");
        }
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.forField(ErrorCode.PHONE_ALREADY_REGISTERED, "phone");
        }
    }

    @Transactional
    public void changePassword(Long userId, String passwordHash, Instant now) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setPasswordHash(passwordHash);
        user.setPasswordChangedAt(now);
    }

    @Transactional
    public void changeStatus(Long userId, com.myapp.aqdeeha.modules.user.entity.UserEnums.UserStatus status) {
        userRepository.findById(userId).orElseThrow().setStatus(status);
    }

    @Transactional
    public void recordLogin(Long userId, Instant now) {
        userRepository.findById(userId).ifPresent(u -> u.setLastLoginAt(now));
    }
}
