package com.youbeam.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuración de los tickets de reproducción ({@code youbeam.jwt.*}).
 *
 * @param privateKeyLocations rutas candidatas a la clave privada PKCS#8 en PEM; se usa la primera que exista
 * @param ephemeralKey        genera una clave temporal en memoria; solo para tests, prohibido en prod
 * @param issuer              valor de {@code iss}: quién emite el ticket
 * @param audience            valor de {@code aud}: quién debe aceptarlo (el Streaming Server)
 * @param ticketTtl           vida del ticket
 */
@ConfigurationProperties(prefix = "youbeam.jwt")
public record JwtProperties(List<String> privateKeyLocations, boolean ephemeralKey, String issuer,
                            String audience, Duration ticketTtl) {
}
