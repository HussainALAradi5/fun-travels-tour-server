package com.server.server.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.auth.AuthResponse;
import com.server.server.dto.user.UserResponse;
import com.server.server.models.User;
import com.server.server.repositories.UserRepository;
import com.server.server.services.JwtService;
import com.server.server.services.UserService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody User user) {
        User savedUser = userService.createUser(user);
        UserResponse userResponse = modelMapper.toUserResponse(savedUser);
        return ResponseEntity.ok(ApiResponse.ok("User registered successfully!", userResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String password = request.get("password");

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));

        User user = userRepository
                .findByEmailIgnoreCaseOrUserNameIgnoreCaseOrMobileNumber(identifier, identifier, identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user.getEmail());

        UserResponse userResponse = modelMapper.toUserResponse(user);
        AuthResponse authResponse = new AuthResponse(token, userResponse);

        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }
}
