package com.myapp.aqdeeha.modules.notification.service;

import com.myapp.aqdeeha.common.api.PageResponse;
import com.myapp.aqdeeha.common.api.Paging;
import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.common.i18n.Messages;
import com.myapp.aqdeeha.modules.notification.dto.NotificationResponse;
import com.myapp.aqdeeha.modules.notification.entity.Notification;
import com.myapp.aqdeeha.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Stores in-app notifications in the DB only (NOT WhatsApp — WhatsApp is reserved for verification codes).
 * Real-time push will be added later via WebSockets. Texts come from {@code messages.properties}:
 * {@code notification.<type>.title} and {@code notification.<type>.body} (body may use {0}, {1}...).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String LINK_COURIER_APPLICATION = "courier_application";

    private final NotificationRepository repository;
    private final Messages messages;
    private final Clock clock;

    @Transactional
    public void notifyUser(Long userId, String type, String linkType, Long linkId, Object... bodyArgs) {
        String title = messages.get("notification." + type + ".title");
        String body = messages.get("notification." + type + ".body", bodyArgs);
        repository.save(Notification.of(userId, type, title, body, linkType, linkId, clock.instant()));
    }

    @Transactional(readOnly = true)
    public NotificationResponse.Inbox inbox(Long userId, Integer page, Integer size) {
        return new NotificationResponse.Inbox(
                PageResponse.of(repository.findByUserIdOrderByIdDesc(userId, Paging.of(page, size)),
                        NotificationResponse::from),
                repository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification n = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        n.markRead(clock.instant());
        return NotificationResponse.from(n);
    }
}
