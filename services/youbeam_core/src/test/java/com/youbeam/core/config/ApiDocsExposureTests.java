package com.youbeam.core.config;

import com.youbeam.core.TestcontainersConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
                    .andExpect(content().string(containsString("YouBeam API")));

            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("YouBeam Core API"))
                    .andExpect(jsonPath("$.components.securitySchemes.sessionCookie.in").value("cookie"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("prod")
    @Import(TestcontainersConfiguration.class)
    class ProdProfile {

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
