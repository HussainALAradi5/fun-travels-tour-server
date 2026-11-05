package com.server.server.services.support;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.constants.errors.WorkflowErrorCodes;
import com.server.server.dto.support.TimelineResponse;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.enums.support.UserRequestStatus;
import com.server.server.exceptions.common.AccessDeniedException;
import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.exceptions.common.WorkflowException;
import com.server.server.models.support.GenericComment;
import com.server.server.models.support.GenericEventLog;
import com.server.server.models.support.UserRequest;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.models.user.User;
import com.server.server.repositories.support.GenericCommentRepository;
import com.server.server.repositories.support.GenericEventLogRepository;
import com.server.server.repositories.support.UserRequestRepository;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.repositories.tourmanagement.TourReservationRepository;
import com.server.server.services.auth.AccessPolicyService;

import lombok.RequiredArgsConstructor;
import com.server.server.dto.support.CommentResponse;
import com.server.server.dto.support.EventLogResponse;

/** Comments and event logs attached to any record type, with per-type access rules. */
@Service
@RequiredArgsConstructor
public class GenericTrackingService {

    private final GenericCommentRepository commentRepository;
    private final GenericEventLogRepository eventLogRepository;
    private final UserRequestRepository userRequestRepository;
    private final TourReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final TourRepository tourRepository;
    private final AccessPolicyService accessPolicy;

    @Transactional(readOnly = true)
    public TimelineResponse getTimeline(@NonNull Integer refId, ReferenceType refType) {
        requireAccess(accessPolicy.requireCurrentUser(), refId, refType);
        return new TimelineResponse(
                getEvents(refId, refType).stream().map(EventLogResponse::from).toList(),
                getComments(refId, refType).stream().map(CommentResponse::from).toList());
    }

    @Transactional
    public GenericComment addComment(@NonNull Integer refId, ReferenceType refType, String content) {
        validateContent(content);
        User author = accessPolicy.requireCurrentUser();
        requireAccess(author, refId, refType);
        requireOpen(refId, refType);
        GenericComment comment = GenericComment.builder()
                .referenceId(refId).referenceType(refType)
                .content(content.trim()).author(author).build();
        return commentRepository.save(comment);
    }

    /** Only the author edits a comment, and only while the record is still open. */
    @Transactional
    public GenericComment updateComment(@NonNull Integer commentId, String newContent) {
        validateContent(newContent);
        GenericComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        User editor = accessPolicy.requireCurrentUser();
        requireAccess(editor, comment.getReferenceId(), comment.getReferenceType());
        requireOpen(comment.getReferenceId(), comment.getReferenceType());
        if (!Objects.equals(comment.getAuthor().getId(), editor.getId())) {
            throw new AccessDeniedException("Only the author can edit this comment.");
        }
        comment.setContent(newContent.trim());
        return commentRepository.save(comment);
    }

    @Transactional
    public void logEvent(@NonNull Integer refId, ReferenceType refType, String action, String description, User actor) {
        eventLogRepository.save(GenericEventLog.builder()
                .referenceId(refId).referenceType(refType)
                .action(action).description(description).actor(actor).build());
    }

    private List<GenericComment> getComments(Integer refId, ReferenceType refType) {
        return commentRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(refId, refType);
    }

    private List<GenericEventLog> getEvents(Integer refId, ReferenceType refType) {
        return eventLogRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(refId, refType);
    }

    private void requireAccess(User user, Integer refId, ReferenceType refType) {
        switch (refType) {
            case USER_REQUEST -> {
                UserRequest request = findUserRequest(refId);
                if (!AccessPolicyService.isSupportStaff(user) && !Objects.equals(request.getUser().getId(), user.getId())) {
                    throw new AccessDeniedException("You are not allowed to access this request timeline.");
                }
            }
            case RESERVATION -> {
                TourReservation reservation = reservationRepository.findById(refId)
                        .orElseThrow(() -> new ResourceNotFoundException("Reservation", refId));
                accessPolicy.requireRecordAccess(user, reservation.getUser().getId(), reservation.getTour().getAgency());
            }
            case TICKET -> {
                Ticket ticket = ticketRepository.findById(refId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket", refId));
                accessPolicy.requireRecordAccess(user, ticket.getCustomer().getId(), ticket.getTour().getAgency());
            }
            case TOUR -> {
                if (AccessPolicyService.isCustomer(user)) {
                    throw new AccessDeniedException("Tour timelines are only available to staff.");
                }
                Tour tour = tourRepository.findById(refId).orElseThrow(() -> new ResourceNotFoundException("Tour", refId));
                accessPolicy.requireAgencyAccess(user, tour.getAgency());
            }
            case USER -> {
                if (!AccessPolicyService.isAdmin(user) && !Objects.equals(user.getId(), refId)) {
                    throw new AccessDeniedException("You are not allowed to access this timeline.");
                }
            }
        }
    }

    private void requireOpen(Integer refId, ReferenceType refType) {
        if (refType != ReferenceType.USER_REQUEST) {
            return;
        }
        UserRequestStatus status = findUserRequest(refId).getStatus();
        if (status == UserRequestStatus.COMPLETED || status == UserRequestStatus.REJECTED) {
            throw new WorkflowException(WorkflowErrorCodes.REQUEST_CLOSED, "This request is closed. No further modifications are allowed.");
        }
    }

    private UserRequest findUserRequest(Integer refId) {
        return userRequestRepository.findById(refId)
                .orElseThrow(() -> new ResourceNotFoundException("Request", refId));
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment content is required.");
        }
    }
}
