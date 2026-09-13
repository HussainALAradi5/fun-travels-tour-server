package com.server.server.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.Notification.ReferenceType;
import com.server.server.enums.UserRequest.UserRequestStatus;
import com.server.server.models.GenericComment;
import com.server.server.models.GenericEventLog;
import com.server.server.models.User;
import com.server.server.models.UserRequest;
import com.server.server.repositories.GenericCommentRepository;
import com.server.server.repositories.GenericEventLogRepository;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.UserRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class GenericTrackingService {

    private final GenericCommentRepository commentRepository;
    private final GenericEventLogRepository eventLogRepository;
    private final UserRepository userRepository;

    // Inject the UserRequestRepository to check statuses
    private final UserRequestRepository userRequestRepository;

    // --- VALIDATION LOGIC ---
    private void validateReferenceState(@NonNull Integer refId, ReferenceType refType) {
        Objects.requireNonNull(refId, "refId must not be null");
        if (refType == ReferenceType.USER_REQUEST) {
            UserRequest request = userRequestRepository.findById(refId)
                    .orElseThrow(() -> new RuntimeException("Reference request not found"));

            if (request.getStatus() == UserRequestStatus.COMPLETED ||
                    request.getStatus() == UserRequestStatus.REJECTED) {
                throw new RuntimeException("This request is closed. No further modifications are allowed.");
            }
        }
        // You can add logic for ReferenceType.TOUR etc. here later
    }

    // --- TIMELINE FETCHING ---
    public Map<String, Object> getTimelineMap(@NonNull Integer refId, ReferenceType refType) {
        Objects.requireNonNull(refId, "refId must not be null");
        Map<String, Object> timelineData = new HashMap<>();
        timelineData.put("events", getEvents(refId, refType));
        timelineData.put("comments", getComments(refId, refType));
        return timelineData;
    }

    // --- COMMENTS ---
    @Transactional
    public GenericComment addComment(@NonNull Integer refId, ReferenceType refType, String content,
            @NonNull Integer authorId) {
        Objects.requireNonNull(refId, "refId must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");
        // Enforce state rule before saving
        validateReferenceState(refId, refType);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found"));

        GenericComment comment = GenericComment.builder()
                .referenceId(refId).referenceType(refType)
                .content(content).author(author).build();

        return commentRepository.save(comment);
    }

    @Transactional
    public GenericComment updateComment(@NonNull Integer commentId, @NonNull Integer editorId, String newContent) {
        Objects.requireNonNull(commentId, "commentId must not be null");
        Objects.requireNonNull(editorId, "editorId must not be null");
        GenericComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Enforce state rule before updating using the comment's stored ref details
        validateReferenceState(comment.getReferenceId(), comment.getReferenceType());

        // Security Check: Only the original author can edit their comment
        if (!comment.getAuthor().getId().equals(editorId)) {
            throw new RuntimeException("Unauthorized: Only the author can edit this comment");
        }

        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new RuntimeException("Editor not found"));

        comment.setContent(newContent);
        comment.setUpdatedBy(editor);

        return commentRepository.save(comment);
    }

    public List<GenericComment> getComments(@NonNull Integer refId, ReferenceType refType) {
        Objects.requireNonNull(refId, "refId must not be null");
        return commentRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(refId, refType);
    }

    // --- EVENT LOGS ---
    @Transactional
    public void logEvent(@NonNull Integer refId, ReferenceType refType, String action, String description, User actor) {
        Objects.requireNonNull(refId, "refId must not be null");
        GenericEventLog log = GenericEventLog.builder()
                .referenceId(refId).referenceType(refType)
                .action(action).description(description).actor(actor).build();

        eventLogRepository.save(log);
    }

    public List<GenericEventLog> getEvents(@NonNull Integer refId, ReferenceType refType) {
        Objects.requireNonNull(refId, "refId must not be null");
        return eventLogRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(refId, refType);
    }
}
