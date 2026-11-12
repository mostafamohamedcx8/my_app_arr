package com.myapp.aqdeeha.modules.audit.service;

import com.myapp.aqdeeha.modules.audit.entity.AuditLog;
import com.myapp.aqdeeha.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * Records admin actions. Joins the caller's transaction (MANDATORY), so the audit row is committed
 * together with the change it describes — or not at all.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String TARGET_COURIER = "courier";
    public static final String TARGET_SHOP = "shop";
    public static final String TARGET_CATEGORY = "category";
    public static final String TARGET_CONFIG = "config";
    public static final String TARGET_TERMS = "terms";

    private final AuditLogRepository repository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long adminId, String action, String targetType, Long targetId, Map<String, ?> details) {
        String json = details == null || details.isEmpty() ? null : jsonMapper.writeValueAsString(details);
        repository.save(AuditLog.of(adminId, action, targetType, targetId, json, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> history(String targetType, Long targetId) {
        return repository.findByTargetTypeAndTargetIdOrderByIdAsc(targetType, targetId);
    }
}
