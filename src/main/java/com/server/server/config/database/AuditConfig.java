package com.server.server.config.database;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.server.server.models.user.User;
import com.server.server.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AuditConfig {

    private final UserRepository userRepository;

    @Bean
    public AuditorAware<User> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getPrincipal() == null
                    || "anonymousUser".equals(authentication.getPrincipal().toString())) {
                return Optional.<User>empty();
            }

            String email;
            if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
            } else {
                email = authentication.getPrincipal().toString();
            }
            return userRepository.findByEmailIgnoreCase(email);
        };
    }
}
