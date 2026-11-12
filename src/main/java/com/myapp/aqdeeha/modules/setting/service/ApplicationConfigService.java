package com.myapp.aqdeeha.modules.setting.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.util.AfterCommit;
import com.myapp.aqdeeha.modules.audit.service.AuditService;
import com.myapp.aqdeeha.modules.setting.dto.ConfigDtos.ConfigResponse;
import com.myapp.aqdeeha.modules.setting.entity.ApplicationConfig;
import com.myapp.aqdeeha.modules.setting.entity.ApplicationConfig.ValueType;
import com.myapp.aqdeeha.modules.setting.repository.ApplicationConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Typed, cached access to {@code application_config}. The whole table is cached as one snapshot for
 * {@code app.config.cache-ttl} and evicted right after a change commits.
 * <p>Getters throw {@link IllegalStateException} (→ 500) when a key is missing, empty or of another type —
 * that is a deployment bug, not a user error. Use {@code find*} for values that may legitimately be unset.
 */
@Service
public class ApplicationConfigService {

    public static final String ACTION_CONFIG_UPDATED = "config.updated";

    private static final String SNAPSHOT = "all";
    private static final BigDecimal MAX_LAT = new BigDecimal("90");
    private static final BigDecimal MAX_LNG = new BigDecimal("180");
    private static final BigDecimal HUNDRED_PERCENT = new BigDecimal("100");

    private record Entry(String value, ValueType type) {
    }

    private final ApplicationConfigRepository repository;
    private final AuditService auditService;
    private final Clock clock;
    private final Cache<String, Map<String, Entry>> cache;

    public ApplicationConfigService(ApplicationConfigRepository repository, AuditService auditService, Clock clock,
                                    @Value("${app.config.cache-ttl:30s}") Duration cacheTtl) {
        this.repository = repository;
        this.auditService = auditService;
        this.clock = clock;
        this.cache = Caffeine.newBuilder().expireAfterWrite(cacheTtl).maximumSize(1).build();
    }

    // ---------- typed getters ----------

    public int getInt(String key) {
        return Integer.parseInt(require(key, ValueType.INT));
    }

    public BigDecimal getDecimal(String key) {
        return new BigDecimal(require(key, ValueType.DECIMAL));
    }

    public String getString(String key) {
        return require(key, ValueType.STRING);
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(require(key, ValueType.BOOLEAN));
    }

    /** Empty when the key exists but has no value yet (e.g. the service area before the admin sets it). */
    public Optional<BigDecimal> findDecimal(String key) {
        Entry e = entry(key, ValueType.DECIMAL);
        return e.value().isBlank() ? Optional.empty() : Optional.of(new BigDecimal(e.value()));
    }

    /** Drops the cached snapshot; the next read reloads from the DB. */
    public void evict() {
        cache.invalidateAll();
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public List<ConfigResponse> list() {
        return repository.findAllByOrderByKeyAsc().stream().map(ApplicationConfigService::toResponse).toList();
    }

    /** Super admin changes one existing key. Value is validated against its type; audited; cache evicted after commit. */
    @Transactional
    public ConfigResponse update(String key, String rawValue, Long adminId) {
        ApplicationConfig row = repository.findByKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_KEY_NOT_FOUND, key));
        String newValue = normalize(row, rawValue.trim());
        String oldValue = row.getValue();
        if (!newValue.equals(oldValue)) {
            row.change(newValue, adminId, clock.instant());
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("key", key);
            details.put("from", oldValue);
            details.put("to", newValue);
            auditService.record(adminId, ACTION_CONFIG_UPDATED, AuditService.TARGET_CONFIG, row.getId(), details);
            AfterCommit.run(this::evict);
        }
        return toResponse(row);
    }

    // ---------- internals ----------

    private String require(String key, ValueType type) {
        Entry e = entry(key, type);
        if (e.value().isBlank()) {
            throw new IllegalStateException("application_config '" + key + "' has no value");
        }
        return e.value();
    }

    private Entry entry(String key, ValueType type) {
        Entry e = snapshot().get(key);
        if (e == null) {
            throw new IllegalStateException("application_config key '" + key + "' is missing");
        }
        if (e.type() != type) {
            throw new IllegalStateException("application_config '" + key + "' is " + e.type() + ", not " + type);
        }
        return e;
    }

    private Map<String, Entry> snapshot() {
        return cache.get(SNAPSHOT, k -> repository.findAll().stream().collect(Collectors.toUnmodifiableMap(
                ApplicationConfig::getKey, c -> new Entry(c.getValue(), c.getValueType()))));
    }

    /** Parses by type, applies simple range rules and returns the canonical text to store. */
    private static String normalize(ApplicationConfig row, String raw) {
        String key = row.getKey();
        try {
            return switch (row.getValueType()) {
                case INT -> {
                    int v = Integer.parseInt(raw);
                    if (v < 0) {
                        throw invalid(key);
                    }
                    yield Integer.toString(v);
                }
                case DECIMAL -> {
                    BigDecimal v = new BigDecimal(raw);
                    if (!decimalInRange(key, v)) {
                        throw invalid(key);
                    }
                    yield v.stripTrailingZeros().toPlainString();
                }
                case BOOLEAN -> {
                    String v = raw.toLowerCase(Locale.ROOT);
                    if (!v.equals("true") && !v.equals("false")) {
                        throw invalid(key);
                    }
                    yield v;
                }
                case STRING -> raw;
            };
        } catch (NumberFormatException e) {
            throw invalid(key);
        }
    }

    private static boolean decimalInRange(String key, BigDecimal v) {
        return switch (key) {
            case ConfigKeys.SERVICE_AREA_LAT -> v.abs().compareTo(MAX_LAT) <= 0;
            case ConfigKeys.SERVICE_AREA_LNG -> v.abs().compareTo(MAX_LNG) <= 0;
            case ConfigKeys.SERVICE_AREA_RADIUS_KM -> v.signum() > 0;
            case ConfigKeys.COMMISSION_RATE -> v.signum() >= 0 && v.compareTo(HUNDRED_PERCENT) <= 0;
            default -> v.signum() >= 0;
        };
    }

    private static BusinessException invalid(String key) {
        return BusinessException.forField(ErrorCode.INVALID_CONFIG_VALUE, "value", key);
    }

    private static ConfigResponse toResponse(ApplicationConfig c) {
        return new ConfigResponse(c.getKey(), c.getValue(), c.getValueType().name(), c.getDescription(),
                c.getUpdatedBy(), c.getUpdatedAt());
    }
}
