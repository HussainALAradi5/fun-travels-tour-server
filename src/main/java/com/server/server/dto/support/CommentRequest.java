package com.server.server.dto.support;

import jakarta.validation.constraints.NotBlank;
public record CommentRequest(
        @NotBlank(message = "Comment content is required") String content) {
}
