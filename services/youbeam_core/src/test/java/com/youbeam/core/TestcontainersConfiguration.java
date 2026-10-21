package com.youbeam.core;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Levanta un PostgreSQL 16 desechable para los tests, inicializado con el mismo
 * esquema que usa {@code deploy/db_init/db.sql}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../../deploy/db_init/db.sql"),
                        "/docker-entrypoint-initdb.d/01-schema.sql");
    }
}
