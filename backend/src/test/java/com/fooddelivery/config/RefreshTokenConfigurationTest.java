package com.fooddelivery.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RefreshTokenConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsRefreshLifetimeAndSecureCookieDefaults() {
        contextRunner.withPropertyValues(
                "app.jwt.secret=" + Base64.getEncoder().encodeToString(new byte[32]),
                "app.jwt.access-ttl-minutes=15",
                "app.jwt.refresh-ttl-days=7",
                "app.auth.refresh-cookie.name=refresh_token",
                "app.auth.refresh-cookie.http-only=true",
                "app.auth.refresh-cookie.secure=true",
                "app.auth.refresh-cookie.same-site=Strict",
                "app.auth.refresh-cookie.path=/api/v1/auth")
                .run(context -> {
                    assertTrue(context.isRunning());
                    assertEquals(7, context.getBean(JwtProperties.class).refreshTtlDays());
                    RefreshCookieProperties cookie = context.getBean(RefreshCookieProperties.class);
                    assertEquals("refresh_token", cookie.name());
                    assertTrue(cookie.httpOnly());
                    assertTrue(cookie.secure());
                    assertEquals("Strict", cookie.sameSite());
                    assertEquals("/api/v1/auth", cookie.path());
                });
    }

    @Test
    void rejectsInsecureRefreshCookieConfiguration() {
        assertThrows(IllegalStateException.class, () -> new RefreshCookieProperties(
                "refresh_token", true, false, "Strict", "/api/v1/auth"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({JwtProperties.class, RefreshCookieProperties.class})
    static class PropertiesConfiguration { }
}
