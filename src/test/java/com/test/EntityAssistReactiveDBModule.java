package com.test;

import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.DatabaseModule;
import com.guicedee.vertxpersistence.annotations.EntityManager;
import com.guicedee.vertxpersistence.implementations.postgres.PostgresConnectionBaseInfo;
import jakarta.validation.constraints.NotNull;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Properties;
import java.util.Set;

@EntityManager(value = "entityAssistReactive", defaultEm = true)
public class EntityAssistReactiveDBModule
        extends DatabaseModule<EntityAssistReactiveDBModule>
        implements IGuiceModule<EntityAssistReactiveDBModule>
{
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("entityassist_test")
            .withUsername("postgres")
            .withPassword("postgres");

    static {
        postgresContainer.start();
    }

    @NotNull
    @Override
    protected String getPersistenceUnitName()
    {
        return "entityAssistReactive";
    }

    @Override
    @NotNull
    protected ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties)
    {
        PostgresConnectionBaseInfo connectionInfo = new PostgresConnectionBaseInfo();
        connectionInfo.setServerName(postgresContainer.getHost());
        connectionInfo.setPort(String.valueOf(postgresContainer.getFirstMappedPort()));
        connectionInfo.setDatabaseName(postgresContainer.getDatabaseName());
        connectionInfo.setUsername(postgresContainer.getUsername());
        connectionInfo.setPassword(postgresContainer.getPassword());
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(true);
        return connectionInfo;
    }

    @NotNull
    @Override
    protected String getJndiMapping()
    {
        return "jdbc:entityAssistReactive";
    }

}
