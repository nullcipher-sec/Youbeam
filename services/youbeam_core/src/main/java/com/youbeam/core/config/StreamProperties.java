package com.youbeam.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Conexión con el Streaming Server ({@code youbeam.stream.*}).
 *
 * @param baseUrl        URL interna del Streaming Server, sin la ruta {@code /offer}
 * @param connectTimeout tiempo máximo para abrir la conexión
 * @param readTimeout    tiempo máximo de respuesta (incluye la recolección de candidatos ICE)
 */
@ConfigurationProperties(prefix = "youbeam.stream")
public record StreamProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
