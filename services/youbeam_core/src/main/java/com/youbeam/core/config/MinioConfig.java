package com.youbeam.core.config;

import io.minio.BucketExistsArgs;
import io.minio.DeleteBucketPolicyArgs;
import io.minio.GetBucketPolicyArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente de MinIO y preparación del bucket al arrancar.
 *
 * El bucket debe ser privado: sin política de bucket, MinIO solo atiende peticiones firmadas con
 * credenciales. Si alguien le añadió una política (p. ej. lectura anónima), se elimina al arrancar.
 * Aunque los archivos ya van cifrados, un bucket público dejaría descargarlos para atacarlos sin
 * límite. Si MinIO no responde la aplicación no arranca, igual que con las claves.
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public MinioClient minioClient(MinioProperties properties) throws Exception {
        if (isBlank(properties.accessKey()) || isBlank(properties.secretKey())) {
            throw new IllegalStateException("MinIO credentials missing: set MINIO_USER and MINIO_PASSWORD");
        }
        MinioClient client = MinioClient.builder()
                .endpoint(properties.url())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
        ensurePrivateBucket(client, properties.bucket());
        return client;
    }

    private static void ensurePrivateBucket(MinioClient client, String bucket) throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            logger.info("MinIO bucket '{}' created", bucket);
        }
        String policy = client.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucket).build());
        if (!isBlank(policy)) {
            client.deleteBucketPolicy(DeleteBucketPolicyArgs.builder().bucket(bucket).build());
            logger.warn("MinIO bucket '{}' had a bucket policy; removed to keep it private", bucket);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
