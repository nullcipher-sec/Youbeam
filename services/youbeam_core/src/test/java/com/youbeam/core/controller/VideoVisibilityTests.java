package com.youbeam.core.controller;

import com.jayway.jsonpath.JsonPath;
import com.youbeam.core.TestcontainersConfiguration;
import com.youbeam.core.support.TestSessions;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de visibilidad: public (catálogo + cualquiera), hidden (solo con enlace), private (solo dueño).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VideoVisibilityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Cookie owner;
    private Cookie stranger;

    @BeforeEach
    void createUsers() throws Exception {
        owner = TestSessions.registerAndLogin(mockMvc);
        stranger = TestSessions.registerAndLogin(mockMvc);
    }

    @Test
    void publicAndHiddenReadyVideosAreVisibleToAnyoneWithTheId() throws Exception {
        String publicId = createReadyVideo("public");
        String hiddenId = createReadyVideo("hidden");

        for (String id : List.of(publicId, hiddenId)) {
            mockMvc.perform(get("/api/videos/{id}", id).cookie(stranger)).andExpect(status().isOk());
            mockMvc.perform(get("/api/videos/{id}", id)).andExpect(status().isOk());
        }
    }

    @Test
    void privateVideoIsOnlyVisibleToItsOwner() throws Exception {
        String id = createReadyVideo("private");

        mockMvc.perform(get("/api/videos/{id}", id).cookie(owner)).andExpect(status().isOk());
        mockMvc.perform(get("/api/videos/{id}", id).cookie(stranger)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/videos/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void videoStillProcessingIsOnlyVisibleToItsOwner() throws Exception {
        String id = createVideo("public");

        mockMvc.perform(get("/api/videos/{id}", id).cookie(owner)).andExpect(status().isOk());
        mockMvc.perform(get("/api/videos/{id}", id).cookie(stranger)).andExpect(status().isNotFound());
    }

    @Test
    void strangerCanSeeButNotModifyAPublicVideo() throws Exception {
        String id = createReadyVideo("public");

        // Ya sabe que existe, así que aquí sí corresponde 403
        mockMvc.perform(patch("/api/videos/{id}", id).cookie(stranger)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/videos/{id}", id).cookie(stranger)).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/videos/{id}", id)).andExpect(jsonPath("$.title").value("Video public"));
    }

    @Test
    void catalogOnlyListsPublicReadyVideosAndNeedsNoSession() throws Exception {
        String publicReady = createReadyVideo("public");
        String hiddenReady = createReadyVideo("hidden");
        String privateReady = createReadyVideo("private");
        String publicProcessing = createVideo("public");
        String publicDeleted = createReadyVideo("public");
        mockMvc.perform(delete("/api/videos/{id}", publicDeleted).cookie(owner)).andExpect(status().isNoContent());

        String json = mockMvc.perform(get("/api/videos/catalog").param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(json, "$[*].id");

        assertThat(ids).contains(publicReady)
                .doesNotContain(hiddenReady, privateReady, publicProcessing, publicDeleted);
    }

    @Test
    void catalogPageSizeIsBounded() throws Exception {
        mockMvc.perform(get("/api/videos/catalog").param("size", "1000")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/videos/catalog").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void publishedAtIsSetTheFirstTimeAVideoBecomesPublic() throws Exception {
        String id = createVideo("private");
        mockMvc.perform(get("/api/videos/{id}", id).cookie(owner)).andExpect(jsonPath("$.publishedAt").isEmpty());

        mockMvc.perform(patch("/api/videos/{id}", id).cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visibility\":\"public\"}"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());
    }

    private String createVideo(String visibility) throws Exception {
        String body = "{\"title\":\"Video %s\",\"visibility\":\"%s\"}".formatted(visibility, visibility);
        String json = mockMvc.perform(post("/api/videos").cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    // La subida cifrada aún no existe: se simula que el procesamiento terminó
    private String createReadyVideo(String visibility) throws Exception {
        String id = createVideo(visibility);
        jdbcTemplate.update("UPDATE videos SET status = 'ready' WHERE id = ?", UUID.fromString(id));
        return id;
    }
}
