package com.myapp.aqdeeha.modules.terms.repository;

import com.myapp.aqdeeha.modules.terms.entity.TermsAcceptance;
import org.springframework.data.repository.Repository;

/** Append-only: exposes only save and reads (no delete / update methods on purpose). */
public interface TermsAcceptanceRepository extends Repository<TermsAcceptance, Long> {

    TermsAcceptance save(TermsAcceptance acceptance);

    boolean existsByUserIdAndTermsVersionId(Long userId, Long termsVersionId);

    long countByTermsVersionId(Long termsVersionId);
}
