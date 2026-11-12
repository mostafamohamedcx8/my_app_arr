package com.myapp.aqdeeha.modules.audit.repository;

import com.myapp.aqdeeha.modules.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTargetTypeAndTargetIdOrderByIdAsc(String targetType, Long targetId);
}
