package com.server.server.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Token signing settings. Startup fails when the secret is missing or too short for HS256. */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "jwt.secret is required")
        @Size(min = 32, message = "jwt.secret must be at least 32 characters") String secret,
        @DefaultValue("86400000") @Positive long expiration) {
}
