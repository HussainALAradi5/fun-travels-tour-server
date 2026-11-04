package com.server.server.controllers.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.auth.AuthResponse;
import com.server.server.dto.auth.LoginRequest;
import com.server.server.dto.auth.PasswordResetConfirmRequest;
import com.server.server.dto.auth.PasswordResetRequest;
import com.server.server.dto.auth.RegisterRequest;
import com.server.server.dto.user.UserResponse;
import com.server.server.services.auth.AuthService;
import com.server.server.utilities.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User registered successfully!",
                UserResponse.from(authService.register(request))));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        String message = authService.requestPasswordReset(request.identifier());
        return ResponseEntity.ok(ApiResponse.ok(message, message));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<UserResponse>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Password reset!",
                UserResponse.from(authService.confirmPasswordReset(request))));
    }
}
