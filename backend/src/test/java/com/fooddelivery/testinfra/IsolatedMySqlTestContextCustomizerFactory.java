package com.fooddelivery.testinfra;

import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.context.ConfigurableApplicationContext;

/** Installs the database-target guard before Spring test contexts are refreshed. */
public final class IsolatedMySqlTestContextCustomizerFactory implements ContextCustomizerFactory {
    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return (ConfigurableApplicationContext context,
                org.springframework.test.context.MergedContextConfiguration ignored) ->
                context.addBeanFactoryPostProcessor(beanFactory -> validateDatabaseContext(context, beanFactory));
    }

    private static void validateDatabaseContext(
            ConfigurableApplicationContext context, ConfigurableListableBeanFactory beanFactory) {
        boolean hasDataSource = beanFactory.getBeanNamesForType(DataSource.class, false, false).length > 0;
        boolean hasFlyway = beanFactory.getBeanNamesForType(Flyway.class, false, false).length > 0;
        if (!hasDataSource && !hasFlyway) {
            return;
        }

        IsolatedMySqlTestDatabaseGuard.validate(
                context.getEnvironment(), System.getenv("FOOD_DELIVERY_TEST_DB_PASSWORD"));
        IsolatedMySqlTestDatabaseGuard.validateInfrastructure(beanFactory);
    }
}
