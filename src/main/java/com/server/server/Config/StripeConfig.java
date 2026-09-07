package com.server.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;
import lombok.Getter; // <-- Allows other classes to read these variables

@Configuration
@Getter // <-- Automatically creates getSecretKey() and getPublishableKey()
public class StripeConfig {

    @Value("${stripe.api.secret-key}")
    private String secretKey;

    @Value("${stripe.api.publishable-key}")
    private String publishableKey;

    @PostConstruct
    public void setup() {
        Stripe.apiKey = secretKey;
    }
}