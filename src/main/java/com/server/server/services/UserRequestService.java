package com.server.server.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.Notification.NotificationType;
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.enums.UserRequest.UserRequestStatus;
import com.server.server.enums.UserRequest.UserRequestType;
import com.server.server.dto.filter.UserRequestFilterRequest;
import com.server.server.enums.UserTypeEnum;
import com.server.server.models.User;
import com.server.server.models.UserRequest;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.UserRequestRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRequestService {

    private final UserRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GenericTrackingService trackingService;

    @Transactional
    public UserRequest create(@NonNull UserRequest request) {
        if (request.getUser() == null || request.getUser().getId() == null) {
            throw new RuntimeException("Request must have a valid user.");
        }
        request.setStatus(UserRequestStatus.PENDING);
        UserRequest saved = requestRepository.save(request);
        notificationService.sendUserRequestNotification(saved, NotificationType.REQUEST_CREATED);
        return saved;
    }

    public UserRequest assignRequest(@NonNull Integer requestId, @NonNull Integer agentId) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        UserRequest request = getById(requestId);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with ID: " + agentId));

        request.setAssignedTo(agent);
        request.setStatus(UserRequestStatus.APPROVED);
        UserRequest updated = requestRepository.save(request);

        trackingService.logEvent(
                updated.getId(),
                ReferenceType.USER_REQUEST,
                "ASSIGNED",
                "Request assigned to " + agent.getName(),
                agent);

        notificationService.sendUserRequestNotification(updated, NotificationType.REQUEST_ASSIGNED);
        return updated;
    }

    @Transactional
    public UserRequest solveRequest(@NonNull Integer requestId, @NonNull Integer solverId) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(solverId, "solverId must not be null");
        UserRequest request = getById(requestId);
        User solver = userRepository.findById(solverId)
                .orElseThrow(() -> new RuntimeException("Solver not found with ID: " + solverId));

        request.setSolvedBy(solver);
        request.setSolvedAt(LocalDateTime.now());
        request.setStatus(UserRequestStatus.COMPLETED);
        UserRequest updated = requestRepository.save(request);

        String action = "SOLVED";
        String description;

        if (request.getUser().getId().equals(solverId)) {
            description = "The requester '" + solver.getName() + "' has closed the request";
        } else {
            description = "Request marked as completed by " + solver.getName();
        }

        trackingService.logEvent(
                updated.getId(),
                ReferenceType.USER_REQUEST,
                action,
                description,
                solver);

        notificationService.sendUserRequestNotification(updated, NotificationType.REQUEST_SOLVED);
        return updated;
    }

    @Transactional
    public UserRequest rejectRequest(@NonNull Integer requestId, @NonNull Integer rejectedById) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(rejectedById, "rejectedById must not be null");
        UserRequest request = getById(requestId);
        User actor = userRepository.findById(rejectedById)
                .orElseThrow(() -> new RuntimeException("Reviewer not found with ID: " + rejectedById));

        request.setStatus(UserRequestStatus.REJECTED);
        request.setSolvedBy(actor);
        request.setSolvedAt(LocalDateTime.now());
        UserRequest updated = requestRepository.save(request);

        trackingService.logEvent(
                updated.getId(),
                ReferenceType.USER_REQUEST,
                "REJECTED",
                "Request was rejected.",
                actor);

        notificationService.sendUserRequestNotification(updated, NotificationType.REQUEST_REJECTED);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<UserRequest> getFilteredRequests(UserRequestFilterRequest filter) {
        Integer currentUserId = filter.getCurrentUserId();
        Objects.requireNonNull(currentUserId, "currentUserId must not be null");
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User session invalid. Please log in again."));

        return requestRepository.findAll((Specification<UserRequest>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currentUser.getUserType() != UserTypeEnum.ADMIN
                    && currentUser.getUserType() != UserTypeEnum.SUPPORT_AGENT) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), currentUserId));
            } else if (filter.getUserIdFilter() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), filter.getUserIdFilter()));
            }

            if (filter.getStatus() != null)
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            if (filter.getType() != null)
                predicates.add(criteriaBuilder.equal(root.get("type"), filter.getType()));
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String term = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), term)));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }, org.springframework.data.domain.Sort.by(
                "asc".equalsIgnoreCase(filter.getSortDir())
                        ? org.springframework.data.domain.Sort.Direction.ASC
                        : org.springframework.data.domain.Sort.Direction.DESC,
                filter.getSortBy() == null || filter.getSortBy().isBlank() ? "createdAt" : filter.getSortBy()));
    }

    public UserRequest getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request #" + id + " not found."));
    }

    public void delete(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        if (!requestRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete: Request #" + id + " does not exist.");
        }
        requestRepository.deleteById(id);
    }
}
