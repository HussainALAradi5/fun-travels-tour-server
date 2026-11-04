package com.server.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
public record PasswordResetRequest(
        @NotBlank(message = "Email is required") String identifier) {
}
