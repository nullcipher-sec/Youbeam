package com.youbeam.core.config;

import com.scalar.maven.webmvc.SpringBootScalarProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Aplica a Scalar la identidad visual de YouBeam (solo cuando Scalar está activo, es decir, en dev).
 * El CSS y el logo viven en {@code resources/scalar/} en lugar de en application.properties
 * para poder editarlos como archivos normales. El favicon se embebe como data URI, así que no
 * hace falta exponer ninguna ruta estática nueva.
 */
@Configuration
@ConditionalOnProperty(name = "scalar.enabled", havingValue = "true")
public class ScalarThemeConfig {

    private static final String THEME_CSS = "scalar/youbeam-theme.css";
    private static final String MARK_SVG = "scalar/youbeam-mark.svg";

    /**
     * {@link SpringBootScalarProperties} tiene scope prototype: cada consumidor recibe su propia
     * instancia, así que no basta con modificar una inyectada. Un BeanPostProcessor completa todas
     * las instancias justo después de que Spring les enlace las propiedades {@code scalar.*}.
     * Es estático para que Spring lo registre antes de crear los demás beans.
     */
    @Bean
    static BeanPostProcessor scalarThemeApplier() throws IOException {
        String css = read(THEME_CSS);
        String favicon = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(read(MARK_SVG).getBytes(StandardCharsets.UTF_8));
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof SpringBootScalarProperties properties) {
                    properties.setCustomCss(css);
                    properties.setFavicon(favicon);
                }
                return bean;
            }
        };
    }

    private static String read(String location) throws IOException {
        return new ClassPathResource(location).getContentAsString(StandardCharsets.UTF_8);
    }
}
