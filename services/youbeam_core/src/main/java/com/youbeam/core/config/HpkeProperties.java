package com.youbeam.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Clave pública HPKE del Streaming Server ({@code youbeam.hpke.*}), con la que el Core envuelve
 * la DEK de cada video.
 *
 * @param publicKeyLocations rutas candidatas al keyset público de Tink en JSON; se usa la primera que exista
 */
@ConfigurationProperties(prefix = "youbeam.hpke")
public record HpkeProperties(List<String> publicKeyLocations) {
}
