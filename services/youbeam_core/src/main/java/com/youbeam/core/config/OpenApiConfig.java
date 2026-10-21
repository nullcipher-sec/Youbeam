package com.youbeam.core.config;

import com.youbeam.core.commons.SessionCookies;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos del documento OpenAPI que consumen Swagger UI y Scalar (solo en el perfil dev).
 * Declara la cookie de sesión como esquema de seguridad para que los endpoints protegidos
 * aparezcan marcados y se puedan probar tras hacer login.
 */
@Configuration
public class OpenApiConfig {

    /** Nombre del esquema de seguridad que referencian los endpoints con {@code @SecurityRequirement}. */
    public static final String SESSION_COOKIE_SCHEME = "sessionCookie";

    @Bean
    public OpenAPI youbeamOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("YouBeam Core API")
                        .version("0.0.1")
                        .description("Authentication, video management and encrypted upload for YouBeam."))
                .components(new Components()
                        .addSecuritySchemes(SESSION_COOKIE_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(SessionCookies.NAME)
                                .description("HttpOnly session cookie set by POST /api/auth/login")));
    }
}
