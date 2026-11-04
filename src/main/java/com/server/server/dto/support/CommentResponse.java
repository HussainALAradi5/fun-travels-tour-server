package com.server.server.dto.support;

import java.time.LocalDateTime;

import com.server.server.dto.user.UserSummary;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.models.support.GenericComment;
/** @param updatedAt set only when the comment was edited after it was posted */
public record CommentResponse(
        Integer id,
        ReferenceType referenceType,
        Integer referenceId,
        String content,
        UserSummary author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CommentResponse from(GenericComment comment) {
        if (comment == null) {
            return null;
        }
        boolean edited = comment.getUpdatedAt() != null && comment.getCreatedAt() != null
                && comment.getUpdatedAt().isAfter(comment.getCreatedAt().plusSeconds(1));
        return new CommentResponse(comment.getId(), comment.getReferenceType(), comment.getReferenceId(),
                comment.getContent(), UserSummary.from(comment.getAuthor()), comment.getCreatedAt(),
                edited ? comment.getUpdatedAt() : null);
    }
}
