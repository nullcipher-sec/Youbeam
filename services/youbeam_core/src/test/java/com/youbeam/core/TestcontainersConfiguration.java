package com.youbeam.core;

import com.youbeam.core.support.TestHpkeKeys;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Levanta un PostgreSQL 16 desechable, inicializado con el mismo esquema que usa
 * {@code deploy/db_init/db.sql}, y un MinIO desechable con la misma imagen que Docker Compose.
 * También apunta el Core a la clave pública HPKE de prueba.
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

    @Bean
    MinIOContainer minioContainer() {
        return new MinIOContainer(DockerImageName.parse("cgr.dev/chainguard/minio:latest")
                .asCompatibleSubstituteFor("minio/minio"));
    }

    @Bean
    DynamicPropertyRegistrar storageProperties(MinIOContainer minio) {
        return registry -> {
            registry.add("youbeam.minio.url", minio::getS3URL);
            registry.add("youbeam.minio.access-key", minio::getUserName);
            registry.add("youbeam.minio.secret-key", minio::getPassword);
            registry.add("youbeam.hpke.public-key-locations", () -> TestHpkeKeys.publicKeyFile().toString());
        };
    }
}
