package com.youbeam.core.controller;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.youbeam.core.TestcontainersConfiguration;
import com.youbeam.core.support.TestSessions;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlaybackTicketTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ECKey ticketSigningKey;

    private Cookie owner;
    private Cookie stranger;

    @BeforeEach
    void createUsers() throws Exception {
        owner = TestSessions.registerAndLogin(mockMvc);
        stranger = TestSessions.registerAndLogin(mockMvc);
    }

    @Test
    void ticketIsAnEs256JwtWithTheExpectedClaims() throws Exception {
        String videoId = createVideo("private", true);
        String ownerId = currentUserId(owner);

        String ticket = playTicket(owner, videoId);
        SignedJWT jwt = SignedJWT.parse(ticket);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(ticketSigningKey.getKeyID());
        assertThat(jwt.verify(new ECDSAVerifier(ticketSigningKey.toPublicJWK()))).isTrue();

        assertThat(claims.getSubject()).isEqualTo(ownerId);
        assertThat(claims.getStringClaim("vid")).isEqualTo(videoId);
        assertThat(UUID.fromString(claims.getJWTID())).isNotNull();
        assertThat(claims.getIssuer()).isEqualTo("youbeam-core");
        assertThat(claims.getAudience()).containsExactly("youbeam-stream");
        Duration lifetime = Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant());
        assertThat(lifetime).isEqualTo(Duration.ofSeconds(60));
        assertThat(claims.getExpirationTime().toInstant()).isAfter(Instant.now());
    }

    @Test
    void ticketResponseIsNeverCached() throws Exception {
        String videoId = createVideo("public", true);

        mockMvc.perform(get("/api/videos/{id}/play", videoId).cookie(owner))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void everyTicketHasAUniqueJti() throws Exception {
        String videoId = createVideo("public", true);

        String first = SignedJWT.parse(playTicket(owner, videoId)).getJWTClaimsSet().getJWTID();
        String second = SignedJWT.parse(playTicket(owner, videoId)).getJWTClaimsSet().getJWTID();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void strangerAndAnonymousGet403ForAPrivateVideo() throws Exception {
        String videoId = createVideo("private", true);

        mockMvc.perform(get("/api/videos/{id}/play", videoId).cookie(stranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ticket").doesNotExist());
        mockMvc.perform(get("/api/videos/{id}/play", videoId)).andExpect(status().isForbidden());
    }

    @Test
    void anonymousViewerOfAPublicVideoGetsAnAnonymousTicket() throws Exception {
        String videoId = createVideo("public", true);

        String json = mockMvc.perform(get("/api/videos/{id}/play", videoId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String ticket = JsonPath.read(json, "$.ticket");

        assertThat(SignedJWT.parse(ticket).getJWTClaimsSet().getSubject()).isEqualTo("anonymous");
    }

    @Test
    void videoNotReadyOrMissingHasNoTicket() throws Exception {
        String processing = createVideo("private", false);

        mockMvc.perform(get("/api/videos/{id}/play", processing).cookie(owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Video is not ready for playback"));
        mockMvc.perform(get("/api/videos/{id}/play", UUID.randomUUID()).cookie(owner))
                .andExpect(status().isNotFound());
    }

    @Test
    void tamperingWithTheVideoIdBreaksTheSignature() throws Exception {
        String videoId = createVideo("public", true);
        String[] parts = playTicket(owner, videoId).split("\\.");

        // Un atacante cambia vid por el id de otro video y conserva la firma original
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        String forgedPayload = payload.replace(videoId, UUID.randomUUID().toString());
        String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(forgedPayload.getBytes())
                + "." + parts[2];

        assertThat(SignedJWT.parse(forged).verify(new ECDSAVerifier(ticketSigningKey.toPublicJWK()))).isFalse();
    }

    private String playTicket(Cookie session, String videoId) throws Exception {
        String json = mockMvc.perform(get("/api/videos/{id}/play", videoId).cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.ticket");
    }

    private String createVideo(String visibility, boolean ready) throws Exception {
        String json = mockMvc.perform(post("/api/videos").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Video\",\"visibility\":\"%s\"}".formatted(visibility)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.id");
        if (ready) {
            // La subida cifrada aún no existe: se simula que el procesamiento terminó
            jdbcTemplate.update("UPDATE videos SET status = 'ready' WHERE id = ?", UUID.fromString(id));
        }
        return id;
    }

    private String currentUserId(Cookie session) throws Exception {
        String json = mockMvc.perform(get("/api/auth/me").cookie(session)).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
