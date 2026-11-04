package com.server.server.dto.support;

import java.time.LocalDateTime;

import com.server.server.dto.user.UserSummary;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.models.support.GenericEventLog;
public record EventLogResponse(
        Integer id,
        ReferenceType referenceType,
        Integer referenceId,
        String action,
        String description,
        UserSummary actor,
        LocalDateTime createdAt) {

    public static EventLogResponse from(GenericEventLog event) {
        if (event == null) {
            return null;
        }
        return new EventLogResponse(event.getId(), event.getReferenceType(), event.getReferenceId(), event.getAction(),
                event.getDescription(), UserSummary.from(event.getActor()), event.getCreatedAt());
    }
}
