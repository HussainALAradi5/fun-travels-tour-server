package com.server.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.server.server.enums.Notification.NotificationType;
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.models.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Integer recipientId);

    long countByRecipientIdAndIsReadFalse(Integer recipientId);

    long countByRecipientIdAndIsReadTrue(Integer recipientId);

    // --- THE DYNAMIC FILTER QUERY ---
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId " +
            "AND (:search IS NULL OR :search = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.message) LIKE LOWER(CONCAT('%', :search, '%'))) "
            +
            "AND (:isRead IS NULL OR n.isRead = :isRead) " +
            "AND (:type IS NULL OR n.type = :type) " +
            "AND (:refType IS NULL OR n.referenceType = :refType) " +
            "AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR n.createdAt >= :startDate) " +
            "AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR n.createdAt < :endDate) " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findFilteredNotifications(
            @Param("userId") Integer userId,
            @Param("search") String search,
            @Param("isRead") Boolean isRead,
            @Param("type") NotificationType type,
            @Param("refType") ReferenceType refType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}