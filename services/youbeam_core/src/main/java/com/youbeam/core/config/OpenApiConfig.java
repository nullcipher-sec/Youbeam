package com.youbeam.core.config;

import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.commons.SessionCookies;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
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

    /** Introducción en Markdown que Scalar muestra como portada de la documentación. */
    private static final String DESCRIPTION = """
            La API del **Core de YouBeam**, la plataforma de video con cifrado de extremo a extremo:
            cuentas, sesiones, gestión de videos y emisión de tickets de reproducción.

            ## Cómo empezar
            1. Crea una cuenta con `POST /api/auth/register`.
            2. Inicia sesión con `POST /api/auth/login`. El servidor responde con la cookie de sesión.
            3. El navegador envía esa cookie en cada petición, así que desde esta página puedes probar
               los endpoints protegidos (marcados con el candado) sin copiar ningún token.

            ## Seguridad
            | Pieza | Cómo se protege |
            |---|---|
            | Contraseñas | Hash Argon2id; nunca se guardan en claro |
            | Sesión | Cookie `HttpOnly`, `Secure` y `SameSite=Strict`; en base de datos solo se guarda su hash |
            | Reproducción | Ticket JWT ES256 de corta duración, válido solo para el Streaming Server |
            | Video | Cifrado AES-256-GCM en reposo y DTLS-SRTP en tránsito |
            """;

    static {
        // AuthenticatedUser lo resuelve el servidor a partir de la cookie; no es un parámetro del cliente
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(AuthenticatedUser.class);
    }

    @Bean
    public OpenAPI youbeamOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("YouBeam Core API")
                        .version("0.0.1")
                        .description(DESCRIPTION))
                .components(new Components()
                        .addSecuritySchemes(SESSION_COOKIE_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(SessionCookies.NAME)
                                .description("Cookie de sesión HttpOnly que establece POST /api/auth/login")));
    }
}
