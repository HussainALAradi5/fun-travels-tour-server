package com.server.server.dto.support;

import com.server.server.enums.support.UserRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record UserRequestCreateRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Description is required") String description,
        @NotNull(message = "Request type is required") UserRequestType type) {
}
