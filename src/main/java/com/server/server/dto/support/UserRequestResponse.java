package com.server.server.dto.support;

import java.time.LocalDateTime;

import com.server.server.dto.user.UserSummary;
import com.server.server.enums.support.UserRequestStatus;
import com.server.server.enums.support.UserRequestType;
import com.server.server.models.support.UserRequest;
public record UserRequestResponse(
        Integer id,
        String title,
        String description,
        UserRequestType type,
        UserRequestStatus status,
        UserSummary user,
        UserSummary assignedTo,
        UserSummary solvedBy,
        LocalDateTime solvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserRequestResponse from(UserRequest request) {
        if (request == null) {
            return null;
        }
        return new UserRequestResponse(request.getId(), request.getTitle(), request.getDescription(), request.getType(),
                request.getStatus(), UserSummary.from(request.getUser()), UserSummary.from(request.getAssignedTo()),
                UserSummary.from(request.getSolvedBy()), request.getSolvedAt(), request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
