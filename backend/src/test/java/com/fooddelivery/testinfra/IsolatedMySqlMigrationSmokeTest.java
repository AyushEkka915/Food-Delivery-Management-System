package com.fooddelivery.testinfra;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fooddelivery.FoodDeliveryApplication;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

/** Starts the real application context so Flyway can initialize the isolated test database. */
@SpringBootTest(classes = FoodDeliveryApplication.class)
@ActiveProfiles("test")
class IsolatedMySqlMigrationSmokeTest {
    private static final String TEST_ONLY_JWT_SECRET = createEphemeralJwtSecret();

    @Autowired
    private ApplicationContext applicationContext;

    @DynamicPropertySource
    static void provideEphemeralJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TEST_ONLY_JWT_SECRET);
    }

    @Test
    void applicationContextStarts() {
        assertNotNull(applicationContext);
    }

    private static String createEphemeralJwtSecret() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
