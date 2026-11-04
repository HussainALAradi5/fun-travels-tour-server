package com.server.server.repositories.support;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.enums.notification.ReferenceType;
import com.server.server.models.support.GenericComment;
public interface GenericCommentRepository extends JpaRepository<GenericComment, Integer> {
    List<GenericComment> findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(Integer referenceId, ReferenceType referenceType);
}
