package com.myapp.aqdeeha.modules.courier.service;

import com.myapp.aqdeeha.common.api.PageResponse;
import com.myapp.aqdeeha.common.api.Paging;
import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.i18n.Messages;
import com.myapp.aqdeeha.common.storage.FileStorageService;
import com.myapp.aqdeeha.modules.courier.entity.FixField;
import java.util.Set;
import com.myapp.aqdeeha.common.util.LowercaseEnumConverter;
import com.myapp.aqdeeha.common.util.PhoneNumbers;
import com.myapp.aqdeeha.modules.admin.entity.Admin;
import com.myapp.aqdeeha.modules.admin.service.AdminService;
import com.myapp.aqdeeha.modules.audit.entity.AuditLog;
import com.myapp.aqdeeha.modules.audit.service.AuditService;
import com.myapp.aqdeeha.modules.auth.service.AuthTokenService;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.ApprovalRequest;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierAdminDetailResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierSummaryResponse;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.DecisionHistoryItem;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile.ApprovalStatus;
import com.myapp.aqdeeha.modules.courier.mapper.CourierMapper;
import com.myapp.aqdeeha.modules.courier.repository.CourierProfileRepository;
import com.myapp.aqdeeha.modules.courier.repository.CourierRow;
import com.myapp.aqdeeha.modules.notification.service.NotificationService;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserStatus;
import com.myapp.aqdeeha.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Admin review of courier applications. Callers are already authorized via {@code @AllowTo(ADMIN)}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourierAdminService {

    public static final String ACTION_APPROVAL_CHANGED = "courier.approval_changed";

    private final CourierProfileRepository repository;
    private final CourierMapper mapper;
    private final UserService userService;
    private final AdminService adminService;
    private final AuthTokenService tokenService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final IdCardUrlSigner idCardUrlSigner;
    private final FileStorageService storage;
    private final JsonMapper jsonMapper;
    private final Messages messages;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<CourierSummaryResponse> list(String status, String q, Integer page, Integer size) {
        ApprovalStatus statusFilter = parseStatusFilter(status);
        String search = Paging.search(q);
        String digits = search == null ? "" : PhoneNumbers.toAsciiDigits(search).replaceAll("\\D", "");
        if (digits.startsWith("0")) {
            digits = digits.substring(1); // 0101... is stored as +20101...
        }
        String phonePattern = digits.isEmpty() ? null : "%" + digits + "%";
        return PageResponse.of(
                repository.search(statusFilter, search, Paging.containsPattern(search), phonePattern, Paging.of(page, size)),
                mapper::toSummary);
    }

    /** Full record incl. signed ID-card links minted for {@code adminId} and the decision history. */
    @Transactional(readOnly = true)
    public CourierAdminDetailResponse detail(Long courierId, Long adminId) {
        return toDetail(load(courierId), adminId);
    }

    /** Decrypts an ID-card image. {@code side} is "front" or "back". */
    @Transactional(readOnly = true)
    public IdCardImage idCard(Long courierId, String side) {
        CourierProfile p = load(courierId).profile();
        String key = switch (side) {
            case "front" -> p.getIdFrontUrl();
            case "back" -> p.getIdBackUrl();
            default -> throw new BusinessException(ErrorCode.NOT_FOUND);
        };
        return new IdCardImage(storage.readEncrypted(key), FileStorageService.mediaTypeOf(key));
    }

    /**
     * approved  → courier can log in (account pending → active).
     * needs_fix → account pending; courier logs in with a limited token to fix and resubmit.
     * rejected  → account pending; courier cannot log in.
     * Leaving "approved" revokes all of the courier's tokens. Suspended / banned accounts keep their status.
     * Every decision is written to audit_log and sent to the courier (in-app + WhatsApp).
     */
    @Transactional
    public CourierAdminDetailResponse decide(Long courierId, ApprovalRequest request, Long adminId) {
        ApprovalStatus target = LowercaseEnumConverter.parse(ApprovalStatus.class, request.status());
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        if (target != ApprovalStatus.APPROVED && note == null) {
            throw BusinessException.forField(ErrorCode.REVIEW_NOTE_REQUIRED, "note");
        }
        Set<FixField> fixFields = target == ApprovalStatus.NEEDS_FIX ? parseFixFields(request.fields()) : Set.of();

        CourierRow row = load(courierId);
        CourierProfile profile = row.profile();
        User user = row.user();
        ApprovalStatus previous = profile.getApprovalStatus();

        profile.setApprovalStatus(target);
        profile.setReviewNote(note);
        profile.setFixFields(FixField.format(fixFields));
        profile.setReviewedBy(adminId);
        profile.setReviewedAt(clock.instant());

        if (target == ApprovalStatus.APPROVED && user.getStatus() == UserStatus.PENDING) {
            userService.changeStatus(user.getId(), UserStatus.ACTIVE);
        } else if (target != ApprovalStatus.APPROVED && user.getStatus() == UserStatus.ACTIVE) {
            userService.changeStatus(user.getId(), UserStatus.PENDING);
        }
        if (previous == ApprovalStatus.APPROVED && target != ApprovalStatus.APPROVED) {
            tokenService.revokeAllForUser(user.getId());
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("from", LowercaseEnumConverter.value(previous));
        details.put("to", LowercaseEnumConverter.value(target));
        details.put("note", note);
        if (!fixFields.isEmpty()) {
            details.put("fields", fixFields.stream().map(FixField::key).toList());
        }
        auditService.record(adminId, ACTION_APPROVAL_CHANGED, AuditService.TARGET_COURIER, courierId, details);

        String fieldLabels = fixFields.stream().map(f -> messages.get("field." + f.key()))
                .collect(Collectors.joining("، "));
        notificationService.notifyUser(user.getId(), "courier_" + LowercaseEnumConverter.value(target),
                NotificationService.LINK_COURIER_APPLICATION, user.getId(), note == null ? "" : note, fieldLabels);
        log.info("Courier {} approval {} -> {} by admin {}", courierId, previous, target, adminId);

        return toDetail(row, adminId);
    }

    private CourierAdminDetailResponse toDetail(CourierRow row, Long adminId) {
        return mapper.toAdminDetail(row, idCardUrlSigner.sign(row.user().getId(), adminId), history(row.user().getId()));
    }

    private List<DecisionHistoryItem> history(Long courierId) {
        List<AuditLog> rows = auditService.history(AuditService.TARGET_COURIER, courierId).stream()
                .filter(a -> ACTION_APPROVAL_CHANGED.equals(a.getAction()))
                .toList();
        Map<Long, String> adminNames = rows.stream().map(AuditLog::getAdminId).distinct()
                .map(adminService::findById).flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(Admin::getId, Admin::getName, (a, b) -> a));
        return rows.stream().map(a -> {
            JsonNode d = a.getDetails() == null ? jsonMapper.createObjectNode() : jsonMapper.readTree(a.getDetails());
            Function<String, String> text = f -> d.hasNonNull(f) ? d.get(f).asString() : null;
            List<String> fields = new java.util.ArrayList<>();
            if (d.has("fields")) {
                d.get("fields").forEach(n -> fields.add(n.asString()));
            }
            return new DecisionHistoryItem(text.apply("from"), text.apply("to"), text.apply("note"), fields,
                    a.getAdminId(), adminNames.get(a.getAdminId()), a.getCreatedAt());
        }).toList();
    }

    private CourierRow load(Long courierId) {
        return repository.findRow(courierId).orElseThrow(() -> new BusinessException(ErrorCode.COURIER_NOT_FOUND));
    }

    /** needs_fix must name at least one valid field. Returned in the fixed FixField order. */
    private static Set<FixField> parseFixFields(List<String> keys) {
        if (keys == null || keys.stream().allMatch(k -> k == null || k.isBlank())) {
            throw BusinessException.forField(ErrorCode.FIX_FIELDS_REQUIRED, "fields");
        }
        Set<FixField> fields = java.util.EnumSet.noneOf(FixField.class);
        for (String k : keys) {
            if (k == null || k.isBlank()) {
                continue;
            }
            fields.add(FixField.fromKey(k.trim())
                    .orElseThrow(() -> BusinessException.forField(ErrorCode.INVALID_FIX_FIELD, "fields", k)));
        }
        return fields;
    }

    private static ApprovalStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return Arrays.stream(ApprovalStatus.values())
                .filter(s -> LowercaseEnumConverter.value(s).equals(status.trim().toLowerCase()))
                .findFirst()
                .orElseThrow(() -> BusinessException.forField(ErrorCode.INVALID_FILTER, "status"));
    }

    public record IdCardImage(byte[] bytes, String mediaType) {
    }
}
