package com.youbeam.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión con el bucket MinIO ({@code youbeam.minio.*}). Las credenciales llegan solo por
 * variables de entorno ({@code deploy/.env}), nunca están en el repositorio.
 *
 * @param url       URL de la API S3 de MinIO (red interna de Docker)
 * @param accessKey usuario de MinIO
 * @param secretKey contraseña de MinIO
 * @param bucket    bucket de los videos cifrados
 */
@ConfigurationProperties(prefix = "youbeam.minio")
public record MinioProperties(String url, String accessKey, String secretKey, String bucket) {
}
