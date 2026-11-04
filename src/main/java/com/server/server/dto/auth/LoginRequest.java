package com.server.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
public record LoginRequest(
        @NotBlank(message = "Email, username, or mobile number is required") String identifier,
        @NotBlank(message = "Password is required") String password) {
}
