package com.server.server.dto.support;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimelineResponse {
    private List<EventLogResponse> events;
    private List<CommentResponse> comments;
}
