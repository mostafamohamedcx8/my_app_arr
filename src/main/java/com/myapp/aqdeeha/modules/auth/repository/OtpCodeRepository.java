package com.myapp.aqdeeha.modules.auth.repository;

import com.myapp.aqdeeha.modules.auth.entity.OtpCode;
import com.myapp.aqdeeha.modules.auth.entity.OtpCode.OtpPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByPhoneAndPurposeOrderByIdDesc(String phone, OtpPurpose purpose);

    /** Locked read so concurrent wrong guesses cannot exceed the attempt limit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpCode> findTopByPhoneAndPurposeOrderByIdDesc(String phone, OtpPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpCode> findByVerificationTokenHash(String verificationTokenHash);

    long countByPhoneAndCreatedAtAfter(String phone, Instant after);

    long countByRequestIpAndCreatedAtAfter(String requestIp, Instant after);
}
