package com.youbeam.core.config;

import com.youbeam.core.TestcontainersConfiguration;
import com.youbeam.core.support.TestKeys;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La documentación de la API describe todos los endpoints, así que solo debe existir en dev.
 */
class ApiDocsExposureTests {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    @Import(TestcontainersConfiguration.class)
    class DevProfile {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void scalarAndOpenApiDocumentAreAvailable() throws Exception {
            mockMvc.perform(get("/scalar"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("YouBeam API")))
                    // El tema propio viaja embebido en la página: CSS de marca y favicon como data URI
                    .andExpect(content().string(containsString("--youbeam-mark")))
                    .andExpect(content().string(containsString("\"favicon\":\"data:image/svg+xml;base64,")));

            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("YouBeam Core API"))
                    .andExpect(jsonPath("$.components.securitySchemes.sessionCookie.in").value("cookie"))
                    // El usuario autenticado lo resuelve el servidor; nunca debe aparecer como parámetro
                    .andExpect(jsonPath("$..parameters[?(@.name == 'user')]").isEmpty());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("prod")
    @Import(TestcontainersConfiguration.class)
    class ProdProfile {

        // En prod la clave temporal está prohibida: se usa una clave real en un PEM, como el Docker secret
        @DynamicPropertySource
        static void signingKey(DynamicPropertyRegistry registry) throws Exception {
            String keyFile = TestKeys.writePkcs8Pem(TestKeys.generate("secp256r1"),
                    Files.createTempFile("jwt_private", ".pem")).toString();
            registry.add("youbeam.jwt.ephemeral-key", () -> "false");
            registry.add("youbeam.jwt.private-key-locations", () -> keyFile);
        }

        @Autowired
        private MockMvc mockMvc;

        @Test
        void apiDocumentationIsNotExposed() throws Exception {
            mockMvc.perform(get("/scalar")).andExpect(status().isNotFound());
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
            mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        }
    }
}
