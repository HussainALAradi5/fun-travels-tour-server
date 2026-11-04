package com.server.server.dto.support;

import java.util.List;
public record TimelineResponse(List<EventLogResponse> events, List<CommentResponse> comments) {
}
