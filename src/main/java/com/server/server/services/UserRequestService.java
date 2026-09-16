package com.server.server.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import com.server.server.dto.PageResponse;
import com.server.server.dto.filter.UserRequestFilterRequest;
import com.server.server.dto.support.UserRequestCreateRequest;
import com.server.server.exceptions.AccessDeniedException;
import com.server.server.enums.Notification.NotificationType;
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.enums.UserRequest.UserRequestAction;
import com.server.server.enums.UserRequest.UserRequestStatus;
import com.server.server.enums.UserTypeEnum;
import com.server.server.models.User;
import com.server.server.models.UserRequest;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.UserRequestRepository;
import com.server.server.utilities.PaginationUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRequestService {

    private final UserRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GenericTrackingService trackingService;
    private final UserService userService;

    @Transactional
    public UserRequest create(@NonNull UserRequestCreateRequest createRequest) {
        UserRequest request = new UserRequest();
        request.setTitle(createRequest.getTitle().trim());
        request.setDescription(createRequest.getDescription().trim());
        request.setType(createRequest.getType());
        request.setUser(userService.getCurrentUser());
        request.setStatus(UserRequestStatus.PENDING);
        UserRequest saved = requestRepository.save(request);
        notificationService.sendUserRequestNotification(saved, NotificationType.REQUEST_CREATED);
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPPORT_AGENT')")
    public UserRequest assignRequest(@NonNull Integer requestId, @NonNull Integer agentId) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        UserRequest request = getById(requestId);
        if (request.getStatus() != UserRequestStatus.PENDING) {
            throw new IllegalStateException("Only a pending request can be assigned.");
        }
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with ID: " + agentId));
        if (agent.getUserType() != UserTypeEnum.SUPPORT_AGENT && agent.getUserType() != UserTypeEnum.ADMIN) {
            throw new IllegalArgumentException("Requests can only be assigned to a support agent or administrator.");
        }

        request.setAssignedTo(agent);
        request.setStatus(UserRequestStatus.APPROVED);
        UserRequest updated = requestRepository.save(request);

        trackingService.logEvent(
                updated.getId(),
                ReferenceType.USER_REQUEST,
                UserRequestAction.ASSIGN.name(),
                "Request assigned to " + agent.getName(),
                userService.getCurrentUser());

        notificationService.sendUserRequestNotification(updated, NotificationType.REQUEST_ASSIGNED);
        return updated;
    }

    @Transactional
    public UserRequest solveRequest(@NonNull Integer requestId) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        UserRequest request = getById(requestId);
        User solver = userService.getCurrentUser();
        boolean requester = request.getUser().getId().equals(solver.getId());
        boolean assignedAgent = request.getAssignedTo() != null
                && request.getAssignedTo().getId().equals(solver.getId());
        boolean privileged = solver.getUserType() == UserTypeEnum.ADMIN
                || solver.getUserType() == UserTypeEnum.SUPPORT_AGENT;
        if (!requester && !assignedAgent && !privileged) {
            throw new AccessDeniedException("You are not allowed to complete this request.");
        }
        if (request.getStatus() != UserRequestStatus.APPROVED) {
            throw new IllegalStateException("Only an assigned request can be completed.");
        }

        request.setSolvedBy(solver);
        request.setSolvedAt(LocalDateTime.now());
        request.setStatus(UserRequestStatus.COMPLETED);
        UserRequest updated = requestRepository.save(request);

        String description;

        if (requester) {
            description = "The requester '" + solver.getName() + "' has closed the request";
        } else {
            description = "Request marked as completed by " + solver.getName();
        }

        trackingService.logEvent(
                updated.getId(),
                ReferenceType.USER_REQUEST,
                UserRequestAction.SOLVE.name(),
                description,
                solver);

        notificationService.sendUserRequestNotification(updated, NotificationType.REQUEST_SOLVED);
        return updated;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPPORT_AGENT')")
    public UserRequest rejectRequest(@NonNull Integer requestId) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        UserRequest request = getById(requestId);
        User actor = userService.getCurrentUser();
        if (request.getStatus() != UserRequestStatus.PENDING
                && request.getStatus() != UserRequestStatus.APPROVED) {
            throw new IllegalStateException("Only a pending or assigned request can be rejected.");
        }

        request.setStatus(UserRequestStatus.REJECTED);
        request.setSolvedBy(actor);
        request.setSolvedAt(LocalDateTime.now());
        UserRequest updated = requestRepository.save(request);

        trackingService.logEvent(
                updated.getId(),
                ReferenceType.USER_REQUEST,
                UserRequestAction.REJECT.name(),
                "Request was rejected.",
                actor);

        notificationService.sendUserRequestNotification(updated, NotificationType.REQUEST_REJECTED);
        return updated;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserRequest> getFilteredRequests(UserRequestFilterRequest filter) {
        User currentUser = userService.getCurrentUser();
        Integer currentUserId = currentUser.getId();

        Specification<UserRequest> specification = (root, query, criteriaBuilder) -> {
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
        };
        return PageResponse.from(requestRepository.findAll(specification,
                PaginationUtils.pageable(filter, "createdAt",
                        Set.of("id", "createdAt", "updatedAt", "status", "type"), java.util.Map.of())));
    }

    public UserRequest getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        UserRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request #" + id + " not found."));
        User currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() != UserTypeEnum.ADMIN
                && currentUser.getUserType() != UserTypeEnum.SUPPORT_AGENT
                && !request.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not allowed to view this request.");
        }
        return request;
    }

    public void delete(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        UserRequest request = getById(id);
        User currentUser = userService.getCurrentUser();
        boolean privileged = currentUser.getUserType() == UserTypeEnum.ADMIN
                || currentUser.getUserType() == UserTypeEnum.SUPPORT_AGENT;
        if (!privileged && (!request.getUser().getId().equals(currentUser.getId())
                || request.getStatus() != UserRequestStatus.PENDING)) {
            throw new AccessDeniedException("Only a pending request can be deleted by its requester.");
        }
        requestRepository.deleteById(id);
    }
}
