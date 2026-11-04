package com.server.server.dto.notification;

import java.time.LocalDateTime;

import com.server.server.enums.notification.NotificationType;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.models.notification.Notification;
public record NotificationResponse(
        Integer id,
        String title,
        String message,
        NotificationType type,
        ReferenceType referenceType,
        Integer referenceId,
        Boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime readAt) {

    public static NotificationResponse from(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.getType(), notification.getReferenceType(), notification.getReferenceId(),
                notification.getIsRead(), notification.getCreatedAt(), notification.getReadAt());
    }
}
