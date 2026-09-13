package com.server.server.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.enums.Notification.ReferenceType;
import com.server.server.models.GenericEventLog;

public interface GenericEventLogRepository extends JpaRepository<GenericEventLog, Integer> {
    List<GenericEventLog> findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(Integer referenceId, ReferenceType referenceType);
}
