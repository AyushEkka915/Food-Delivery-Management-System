package com.fooddelivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long accessTtlMinutes, long refreshTtlDays) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must contain a Base64-encoded HS256 key");
        }
        if (accessTtlMinutes != 15) {
            throw new IllegalStateException("JWT access token lifetime must be 15 minutes");
        }
        if (refreshTtlDays < 1) {
            throw new IllegalStateException("JWT refresh token lifetime must be at least one day");
        }
    }
}
