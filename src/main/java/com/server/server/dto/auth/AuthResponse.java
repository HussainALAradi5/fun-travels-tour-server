package com.server.server.dto.auth;

import com.server.server.dto.user.UserResponse;
public record AuthResponse(String token, UserResponse user) {
}
