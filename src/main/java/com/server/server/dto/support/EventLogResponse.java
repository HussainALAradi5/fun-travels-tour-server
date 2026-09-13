package com.server.server.dto.support;

import java.time.LocalDateTime;

import com.server.server.enums.Notification.ReferenceType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventLogResponse {
    private Integer id;
    private ReferenceType referenceType;
    private Integer referenceId;
    private String action;
    private String description;
    private UserSummary actor;
    private LocalDateTime createdAt;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class UserSummary {
        private Integer id;
        private String name;
    }
}
