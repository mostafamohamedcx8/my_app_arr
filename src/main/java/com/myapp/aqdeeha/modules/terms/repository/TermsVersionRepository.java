package com.myapp.aqdeeha.modules.terms.repository;

import com.myapp.aqdeeha.modules.terms.entity.TermsVersion;
import com.myapp.aqdeeha.modules.terms.entity.TermsVersion.Audience;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TermsVersionRepository extends JpaRepository<TermsVersion, Long> {

    Optional<TermsVersion> findByAudienceAndCurrentTrue(Audience audience);

    /** Locks the current row so two admins can't publish the same version number at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TermsVersion t where t.audience = :audience and t.current = true")
    Optional<TermsVersion> lockCurrent(Audience audience);

    @Query("select coalesce(max(t.version), 0) from TermsVersion t where t.audience = :audience")
    int maxVersion(Audience audience);

    List<TermsVersion> findByAudienceOrderByVersionDesc(Audience audience);
}
