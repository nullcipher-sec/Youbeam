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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VideoControllerTests {

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
    void createdVideoIsPrivateAndPendingUploadByDefault() throws Exception {
        create(owner, "{\"title\":\"  My first video  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("My first video"))
                .andExpect(jsonPath("$.visibility").value("private"))
                .andExpect(jsonPath("$.status").value("processing"));
    }

    @Test
    void ownerCanReadListAndUpdateOwnVideo() throws Exception {
        String id = createAndGetId(owner, "{\"title\":\"Original\",\"visibility\":\"hidden\"}");

        mockMvc.perform(get("/api/videos/{id}", id).cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("hidden"));
        mockMvc.perform(get("/api/videos").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        update(owner, id, "{\"title\":\"Renamed\",\"visibility\":\"public\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.visibility").value("public"))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void listOnlyContainsOwnVideos() throws Exception {
        createAndGetId(owner, "{\"title\":\"Owner video\"}");
        createAndGetId(stranger, "{\"title\":\"Stranger video\"}");

        mockMvc.perform(get("/api/videos").cookie(owner))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Owner video"));
    }

    @Test
    void strangerCannotSeeUpdateOrDeleteSomeoneElsesVideo() throws Exception {
        String id = createAndGetId(owner, "{\"title\":\"Owner only\"}");

        // 404 y no 403: no se confirma que el video exista
        mockMvc.perform(get("/api/videos/{id}", id).cookie(stranger)).andExpect(status().isNotFound());
        update(stranger, id, "{\"title\":\"Hacked\"}").andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/videos/{id}", id).cookie(stranger)).andExpect(status().isNotFound());

        String title = jdbcTemplate.queryForObject("SELECT title FROM videos WHERE id = ?::uuid", String.class, id);
        assertThat(title).isEqualTo("Owner only");
    }

    @Test
    void deleteIsSoftAndHidesTheVideo() throws Exception {
        String id = createAndGetId(owner, "{\"title\":\"To delete\"}");

        mockMvc.perform(delete("/api/videos/{id}", id).cookie(owner)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/videos/{id}", id).cookie(owner)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/videos").cookie(owner)).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(delete("/api/videos/{id}", id).cookie(owner)).andExpect(status().isNotFound());

        Boolean softDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM videos WHERE id = ?::uuid", Boolean.class, id);
        assertThat(softDeleted).isTrue();
    }

    // GET /api/videos/{id} y el catálogo son públicos: se prueban en VideoVisibilityTests
    @Test
    void writeAndOwnListEndpointsRequireSession() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/videos").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/videos")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/videos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/videos/{id}", id)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        create(owner, "{\"title\":\"   \"}").andExpect(status().isBadRequest());
        create(owner, "{\"title\":\"x\",\"visibility\":\"secret\"}").andExpect(status().isBadRequest());
        create(owner, "{\"title\":\"" + "a".repeat(201) + "\"}").andExpect(status().isBadRequest());

        String id = createAndGetId(owner, "{\"title\":\"Valid\"}");
        update(owner, id, "{\"title\":\"  \"}").andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/videos/not-a-uuid").cookie(owner)).andExpect(status().isBadRequest());
    }

    private ResultActions create(Cookie session, String body) throws Exception {
        return mockMvc.perform(post("/api/videos").cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(Cookie session, String id, String body) throws Exception {
        return mockMvc.perform(patch("/api/videos/{id}", id).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String createAndGetId(Cookie session, String body) throws Exception {
        String json = create(session, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
