package com.server.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record PasswordResetConfirmRequest(
        @NotBlank(message = "Email is required") String identifier,
        @NotBlank(message = "Reset token is required") String token,
        @NotBlank(message = "New password is required") @Size(min = 6, message = "Password must be at least 6 characters") String newPassword) {
}
