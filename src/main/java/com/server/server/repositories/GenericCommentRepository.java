package com.server.server.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.enums.Notification.ReferenceType;
import com.server.server.models.GenericComment;

public interface GenericCommentRepository extends JpaRepository<GenericComment, Integer> {
    List<GenericComment> findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(Integer referenceId, ReferenceType referenceType);
}
