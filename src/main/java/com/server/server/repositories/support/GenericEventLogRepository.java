package com.server.server.repositories.support;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.enums.notification.ReferenceType;
import com.server.server.models.support.GenericEventLog;
public interface GenericEventLogRepository extends JpaRepository<GenericEventLog, Integer> {
    List<GenericEventLog> findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(Integer referenceId, ReferenceType referenceType);
}
