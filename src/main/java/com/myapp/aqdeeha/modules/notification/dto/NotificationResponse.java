package com.myapp.aqdeeha.modules.notification.dto;

import com.myapp.aqdeeha.common.api.PageResponse;
import com.myapp.aqdeeha.modules.notification.entity.Notification;

import java.time.Instant;

public record NotificationResponse(Long id, String type, String title, String body, String linkType, Long linkId,
                                   boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLinkType(),
                n.getLinkId(), n.getReadAt() != null, n.getCreatedAt());
    }

    /** Page of notifications plus the total unread count (for the app badge). */
    public record Inbox(PageResponse<NotificationResponse> notifications, long unreadCount) {
    }
}
