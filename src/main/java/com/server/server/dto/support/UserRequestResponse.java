package com.server.server.dto.support;

import java.time.LocalDateTime;

import com.server.server.enums.UserRequest.UserRequestStatus;
import com.server.server.enums.UserRequest.UserRequestType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestResponse {
    private Integer id;
    private String title;
    private String description;
    private UserRequestType type;
    private UserRequestStatus status;
    private UserSummary user;
    private UserSummary assignedTo;
    private UserSummary solvedBy;
    private LocalDateTime solvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class UserSummary {
        private Integer id;
        private String name;
    }
}
