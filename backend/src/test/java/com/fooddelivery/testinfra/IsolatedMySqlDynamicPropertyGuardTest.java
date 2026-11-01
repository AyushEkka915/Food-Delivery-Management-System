package com.fooddelivery.testinfra;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContextManager;

class IsolatedMySqlDynamicPropertyGuardTest {
    @Test
    void dynamicDatasourceOverrideIsRejectedBeforeDatasourceBeanInitialization() {
        DataSourceConfiguration.CREATED.set(false);
        TestContextManager manager = new TestContextManager(DynamicOverrideContext.class);

        Throwable failure = assertThrows(Throwable.class,
                () -> manager.getTestContext().getApplicationContext());

        assertTrue(hasCauseMessage(failure, "Unsafe test datasource"));
        assertFalse(DataSourceConfiguration.CREATED.get(), "datasource bean must not initialize before guard rejection");
    }

    private static boolean hasCauseMessage(Throwable failure, String text) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    @ActiveProfiles("test")
    @ContextConfiguration(classes = DataSourceConfiguration.class)
    static class DynamicOverrideContext {
        @DynamicPropertySource
        static void overrideDatasource(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", () -> "jdbc:mysql://localhost:3306/food_delivery");
            registry.add("spring.datasource.username", () -> "foodapp");
            registry.add("spring.datasource.password", () -> "non-secret-test-value");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DataSourceConfiguration {
        private static final AtomicBoolean CREATED = new AtomicBoolean();

        @Bean
        DataSource dataSource() {
            CREATED.set(true);
            return new DriverManagerDataSource();
        }
    }
}
