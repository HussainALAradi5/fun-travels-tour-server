package com.server.server.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.models.User; // Ensure you have spring-boot-starter-validation
import com.server.server.repositories.UserRepository;
import com.server.server.services.JwtService;
import com.server.server.services.UserService;

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

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody User user) {
        try {
            // Check for empty critical fields manually if not using @Valid annotations in
            // Model
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                throw new RuntimeException("Email is required");
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new RuntimeException("Password is required");
            }

            User savedUser = userService.createUser(user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User registered successfully!",
                    "data", savedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String identifier = request.get("identifier");
            String password = request.get("password");

            // 1. Authenticate with Spring Security (uses the updated UserDetailsService)
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));

            // 2. Find the user to get their actual email for the JWT
            User user = userRepository
                    .findByEmailIgnoreCaseOrUserNameIgnoreCaseOrMobileNumber(identifier, identifier, identifier)
                    .orElseThrow(() -> new RuntimeException("User data not found after authentication"));

            // 3. Generate token using the user's primary email
            String token = jwtService.generateToken(user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Invalid email/username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}