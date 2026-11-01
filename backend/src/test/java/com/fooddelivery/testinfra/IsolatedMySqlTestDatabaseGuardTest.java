package com.fooddelivery.testinfra;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class IsolatedMySqlTestDatabaseGuardTest {
    @Test
    void acceptsOnlyConfiguredIsolatedTestDatabase() {
        MockEnvironment environment = safeEnvironment();

        assertDoesNotThrow(() -> validate(environment));
    }

    @Test
    void rejectsDevelopmentDatabase() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/food_delivery");

        assertThrows(IllegalStateException.class, () -> validate(environment));
    }

    @Test
    void rejectsUnexpectedHostOrPort() {
        MockEnvironment wrongHost = safeEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/food_delivery_test");
        MockEnvironment wrongPort = safeEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3307/food_delivery_test");

        assertThrows(IllegalStateException.class, () -> validate(wrongHost));
        assertThrows(IllegalStateException.class, () -> validate(wrongPort));
    }

    @Test
    void rejectsAnyOtherDatabaseAndMissingEnvironmentPassword() {
        MockEnvironment wrongDatabase = safeEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/other_db");

        assertThrows(IllegalStateException.class, () -> validate(wrongDatabase));
        assertThrows(IllegalStateException.class,
                () -> IsolatedMySqlTestDatabaseGuard.validate(safeEnvironment(), null));
    }

    @Test
    void rejectsWrongPrimaryUsername() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("spring.datasource.username", "foodapp");

        assertThrows(IllegalStateException.class, () -> validate(environment));
    }

    @Test
    void rejectsHikariJdbcUrlAndCredentialOverrides() {
        MockEnvironment wrongUrl = safeEnvironment()
                .withProperty("spring.datasource.hikari.jdbc-url", "jdbc:mysql://localhost:3306/food_delivery");
        MockEnvironment wrongUsername = safeEnvironment()
                .withProperty("spring.datasource.hikari.username", "foodapp");
        MockEnvironment wrongPassword = safeEnvironment()
                .withProperty("spring.datasource.hikari.password", "different-secret");

        assertThrows(IllegalStateException.class, () -> validate(wrongUrl));
        assertThrows(IllegalStateException.class, () -> validate(wrongUsername));
        assertThrows(IllegalStateException.class, () -> validate(wrongPassword));
    }

    @Test
    void rejectsIndependentFlywayConnectionOverrides() {
        for (String property : new String[] {
                "spring.flyway.url", "spring.flyway.user", "spring.flyway.password"}) {
            MockEnvironment environment = safeEnvironment().withProperty(property, "configured");
            assertThrows(IllegalStateException.class, () -> validate(environment), property);
        }
    }

    @Test
    void rejectsConflictingHigherPriorityPropertySource() {
        MockEnvironment environment = safeEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-override",
                Map.of("spring.datasource.url", "jdbc:mysql://localhost:3306/food_delivery")));

        assertThrows(IllegalStateException.class, () -> validate(environment));
    }

    @Test
    void nonDatabaseSpringTestContextDoesNotRequireTestDatabaseConfiguration() {
        var factory = new IsolatedMySqlTestContextCustomizerFactory();
        var customizer = factory.createContextCustomizer(getClass(), java.util.List.of());
        try (var context = new AnnotationConfigApplicationContext()) {
            assertDoesNotThrow(() -> customizer.customizeContext(context, null));
            assertDoesNotThrow(context::refresh);
        }
    }

    @Test
    void rejectsWhenTestProfileIsNotActive() {
        MockEnvironment environment = safeEnvironment();
        environment.setActiveProfiles("dev");

        assertThrows(IllegalStateException.class, () -> validate(environment));
    }

    private static MockEnvironment safeEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        return environment
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/food_delivery_test")
                .withProperty("spring.datasource.username", "foodapp_test")
                .withProperty("spring.datasource.password", TEST_PASSWORD);
    }

    private static final String TEST_PASSWORD = "unit-test-placeholder";

    private static void validate(MockEnvironment environment) {
        IsolatedMySqlTestDatabaseGuard.validate(environment, TEST_PASSWORD);
    }
}
