package com.server.server.controllers.notification;

import com.server.server.services.auth.AccessPolicyService;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.notification.NotificationCounts;
import com.server.server.dto.notification.NotificationResponse;
import com.server.server.enums.notification.NotificationType;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.models.notification.Notification;
import com.server.server.services.notification.NotificationService;
import com.server.server.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final AccessPolicyService accessPolicy;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @NonNull @PathVariable Integer userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) ReferenceType refType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        accessPolicy.requireSelfOrAdmin(userId);
        List<Notification> notifications = notificationService.filterUserNotifications(
                userId, search, isRead, type, refType, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(notifications.stream().map(NotificationResponse::from).toList()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@NonNull @PathVariable Integer id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok(NotificationResponse.from(notification)));
    }

    @GetMapping("/user/{userId}/counts")
    public ResponseEntity<ApiResponse<NotificationCounts>> getNotificationCounts(@NonNull @PathVariable Integer userId) {
        accessPolicy.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getNotificationCounts(userId)));
    }
}
