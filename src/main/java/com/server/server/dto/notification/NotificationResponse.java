package com.server.server.dto.notification;

import java.time.LocalDateTime;

import com.server.server.enums.Notification.NotificationType;
import com.server.server.enums.Notification.ReferenceType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private Integer id;
    private String title;
    private String message;
    private NotificationType type;
    private ReferenceType referenceType;
    private Integer referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
