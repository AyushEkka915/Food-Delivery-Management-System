package com.fooddelivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
public record RefreshCookieProperties(String name, boolean httpOnly, boolean secure, String sameSite, String path) {
    public RefreshCookieProperties {
        if (name == null || !name.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalStateException("Refresh cookie name is invalid");
        }
        if (!httpOnly || !secure) {
            throw new IllegalStateException("Refresh cookie must be HttpOnly and Secure");
        }
        if (!"Strict".equals(sameSite) && !"Lax".equals(sameSite) && !"None".equals(sameSite)) {
            throw new IllegalStateException("Refresh cookie SameSite must be Strict, Lax, or None");
        }
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalStateException("SameSite=None refresh cookies require Secure=true");
        }
        if (path == null || !path.startsWith("/")) {
            throw new IllegalStateException("Refresh cookie path must be an absolute path");
        }
    }
}
