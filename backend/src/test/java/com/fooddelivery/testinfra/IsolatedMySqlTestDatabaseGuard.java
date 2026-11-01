package com.fooddelivery.testinfra;

import java.net.URI;
import java.net.URISyntaxException;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

/** Validates the target before Spring creates a test datasource or runs Flyway. */
final class IsolatedMySqlTestDatabaseGuard {
    private static final String EXPECTED_URL_PREFIX = "jdbc:mysql:";
    private static final String EXPECTED_HOST = "localhost";
    private static final int EXPECTED_PORT = 3306;
    private static final String EXPECTED_DATABASE = "/food_delivery_test";
    private static final String EXPECTED_URL =
            "jdbc:mysql://localhost:3306/food_delivery_test";
    private static final String EXPECTED_USERNAME = "foodapp_test";
    private static final String TEST_PASSWORD_ENV = "FOOD_DELIVERY_TEST_DB_PASSWORD";
    private static final String EXPECTED_DATA_SOURCE_FACTORY =
            "org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari";
    private static final String EXPECTED_FLYWAY_FACTORY =
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration$FlywayConfiguration";

    private IsolatedMySqlTestDatabaseGuard() { }

    static void validate(ConfigurableEnvironment environment, String suppliedTestPassword) {
        if (!environment.acceptsProfiles(Profiles.of("test"))) {
            throw new IllegalStateException(
                    "Database integration tests must activate the 'test' profile; refusing datasource startup.");
        }

        String url = environment.getProperty("spring.datasource.url", "").trim();
        if (!EXPECTED_URL.equals(url) || !isExpectedUrl(url)) {
            throw new IllegalStateException(
                    "Unsafe test datasource. Expected MySQL at localhost:3306/food_delivery_test.");
        }

        String username = environment.getProperty("spring.datasource.username", "");
        if (!EXPECTED_USERNAME.equals(username)) {
            throw new IllegalStateException("Unsafe test datasource username; expected foodapp_test.");
        }

        if (suppliedTestPassword == null || suppliedTestPassword.isBlank()) {
            throw new IllegalStateException(
                    TEST_PASSWORD_ENV + " must be set in the process environment.");
        }
        if (!suppliedTestPassword.equals(environment.getProperty("spring.datasource.password"))) {
            throw new IllegalStateException(
                    "Test datasource password must come directly from " + TEST_PASSWORD_ENV + ".");
        }

        requireOptionalValue(environment, "spring.datasource.hikari.jdbc-url", EXPECTED_URL);
        requireOptionalValue(environment, "spring.datasource.hikari.username", EXPECTED_USERNAME);
        requireOptionalValue(environment, "spring.datasource.hikari.password", suppliedTestPassword);

        rejectConfigured(environment, "spring.flyway.url", "Independent Flyway URLs are not allowed in tests.");
        rejectConfigured(environment, "spring.flyway.user", "Independent Flyway credentials are not allowed in tests.");
        rejectConfigured(environment, "spring.flyway.password", "Independent Flyway credentials are not allowed in tests.");

        rejectConfigured(environment, "spring.datasource.jndi-name", "JNDI datasources are not allowed in tests.");
        rejectConfigured(environment, "spring.datasource.type", "Custom datasource types are not allowed in tests.");
        rejectConfigured(environment, "spring.datasource.hikari.data-source-class-name",
                "Hikari data-source-class-name overrides are not allowed in tests.");
        rejectHikariDataSourceProperties(environment);
    }

    static void validateInfrastructure(ConfigurableListableBeanFactory beanFactory) {
        String[] dataSources = beanFactory.getBeanNamesForType(DataSource.class, false, false);
        if (dataSources.length != 1 || !"dataSource".equals(dataSources[0])) {
            throw new IllegalStateException("Test contexts must use only the guarded Spring Boot datasource.");
        }
        requireFactory(beanFactory, dataSources[0], EXPECTED_DATA_SOURCE_FACTORY);

        String[] flyways = beanFactory.getBeanNamesForType(org.flywaydb.core.Flyway.class, false, false);
        if (flyways.length > 1) {
            throw new IllegalStateException("Multiple Flyway instances are not allowed in test contexts.");
        }
        if (flyways.length == 1) {
            requireFactory(beanFactory, flyways[0], EXPECTED_FLYWAY_FACTORY);
        }

        String[] annotatedFlywayDataSources = beanFactory.getBeanNamesForAnnotation(
                org.springframework.boot.autoconfigure.flyway.FlywayDataSource.class);
        if (annotatedFlywayDataSources.length > 0) {
            throw new IllegalStateException("Separate Flyway datasources are not allowed in test contexts.");
        }
    }

    private static void requireFactory(
            ConfigurableListableBeanFactory beanFactory, String beanName, String expectedFactory) {
        BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
        if (!expectedFactory.equals(definition.getFactoryBeanName())) {
            throw new IllegalStateException("Custom datasource or Flyway bean definitions are not allowed in tests.");
        }
    }

    private static void requireOptionalValue(
            ConfigurableEnvironment environment, String propertyName, String expectedValue) {
        if (environment.containsProperty(propertyName)
                && !expectedValue.equals(environment.getProperty(propertyName))) {
            throw new IllegalStateException("Conflicting test datasource property: " + propertyName + ".");
        }
    }

    private static void rejectConfigured(
            ConfigurableEnvironment environment, String propertyName, String message) {
        if (environment.containsProperty(propertyName)) {
            String value = environment.getProperty(propertyName);
            if (value != null && !value.isBlank()) {
                throw new IllegalStateException(message);
            }
        }
    }

    private static void rejectHikariDataSourceProperties(ConfigurableEnvironment environment) {
        String prefix = "spring.datasource.hikari.data-source-properties.";
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource<?> enumerable) {
                for (String propertyName : enumerable.getPropertyNames()) {
                    if (propertyName.startsWith(prefix)) {
                        Object value = propertySource.getProperty(propertyName);
                        if (value != null && !value.toString().isBlank()) {
                            throw new IllegalStateException(
                                    "Hikari driver properties are not allowed to override the test target.");
                        }
                    }
                }
            }
        }
    }

    private static boolean isExpectedUrl(String url) {
        if (!url.startsWith(EXPECTED_URL_PREFIX)) {
            return false;
        }
        try {
            URI uri = new URI(url.substring(EXPECTED_URL_PREFIX.length()));
            return EXPECTED_HOST.equals(uri.getHost())
                    && EXPECTED_PORT == uri.getPort()
                    && EXPECTED_DATABASE.equals(uri.getPath())
                    && uri.getUserInfo() == null
                    && uri.getQuery() == null
                    && uri.getFragment() == null;
        } catch (URISyntaxException | IllegalArgumentException malformedUrl) {
            return false;
        }
    }
}
