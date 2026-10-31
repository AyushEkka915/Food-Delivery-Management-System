package com.fooddelivery.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieFactoryTest {
    @Test
    void createsConfiguredHttpOnlySecureCookieWithSevenDayMaxAge() {
        RefreshCookieProperties cookieProperties = new RefreshCookieProperties(
                "refresh_token", true, true, "Strict", "/api/v1/auth");
        JwtProperties jwtProperties = new JwtProperties("test-key", 15, 7);
        RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory(cookieProperties, jwtProperties);

        ResponseCookie cookie = factory.create("opaque-test-value");

        assertEquals("refresh_token", cookie.getName());
        assertEquals("opaque-test-value", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/api/v1/auth", cookie.getPath());
        assertEquals(Duration.ofDays(7), cookie.getMaxAge());
    }
}
