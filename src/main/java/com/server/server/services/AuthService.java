package com.server.server.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String authenticate(String email, String password) {
        // This checks the password via BCrypt automatically
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        // If we reach here, password is valid
        return jwtService.generateToken(email);
    }
}
