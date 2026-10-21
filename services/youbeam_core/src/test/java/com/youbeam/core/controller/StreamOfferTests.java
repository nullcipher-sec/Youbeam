package com.youbeam.core.controller;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.youbeam.core.TestcontainersConfiguration;
import com.youbeam.core.support.TestKeys;
import com.youbeam.core.support.TestSessions;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/stream/offer contra un Streaming Server simulado que imita a services/youbeam_stream/signaling.go.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StreamOfferTests {

    private static final String OFFER_SDP = "v=0\r\no=- 1 2 IN IP4 127.0.0.1\r\ns=-\r\n";
    private static final String ANSWER_SDP = "v=0\r\no=- 3 4 IN IP4 127.0.0.1\r\ns=answer\r\n";

    private static final HttpServer fakeStream;
    private static final AtomicInteger calls = new AtomicInteger();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();
    private static volatile int responseStatus = 200;
    private static volatile long responseDelayMs = 0;

    static {
        try {
            fakeStream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        fakeStream.createContext("/offer", exchange -> {
            calls.incrementAndGet();
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(responseDelayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            byte[] body = responseStatus == 200
                    ? ("{\"type\":\"answer\",\"sdp\":\"" + ANSWER_SDP.replace("\r\n", "\\r\\n") + "\"}").getBytes()
                    : "Ticket inválido".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", responseStatus == 200 ? "application/json" : "text/plain");
            exchange.sendResponseHeaders(responseStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        fakeStream.start();
    }

    @DynamicPropertySource
    static void streamUrl(DynamicPropertyRegistry registry) {
        registry.add("youbeam.stream.base-url", () -> "http://127.0.0.1:" + fakeStream.getAddress().getPort());
        registry.add("youbeam.stream.read-timeout", () -> "1s");
    }

    @AfterAll
    static void stopFakeStream() {
        fakeStream.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ECKey ticketSigningKey;

    private Cookie owner;

    @BeforeEach
    void reset() throws Exception {
        calls.set(0);
        lastBody.set(null);
        responseStatus = 200;
        responseDelayMs = 0;
        owner = TestSessions.registerAndLogin(mockMvc);
    }

    @Test
    void validOfferIsForwardedAndTheAnswerReturned() throws Exception {
        String ticket = issueTicket();

        offer(OFFER_SDP, ticket)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.type").value("answer"))
                .andExpect(jsonPath("$.sdp").value(ANSWER_SDP));

        assertThat(calls.get()).isOne();
        assertThat((String) JsonPath.read(lastBody.get(), "$.sdp")).isEqualTo(OFFER_SDP);
        assertThat((String) JsonPath.read(lastBody.get(), "$.ticket")).isEqualTo(ticket);
    }

    @Test
    void forgedExpiredOrMisdirectedTicketsNeverReachTheStreamServer() throws Exception {
        // Firmado con una clave que no es la del Core: así fabricaría tickets un atacante
        String otherKeyTicket = sign(attackerKey(), claims("youbeam-stream", Instant.now().plusSeconds(60)));
        String expired = sign(ticketSigningKey, claims("youbeam-stream", Instant.now().minusSeconds(1)));
        String wrongAudience = sign(ticketSigningKey, claims("another-service", Instant.now().plusSeconds(60)));

        for (String ticket : new String[]{"not-a-jwt", otherKeyTicket, expired, wrongAudience}) {
            offer(OFFER_SDP, ticket)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid or expired playback ticket"));
        }
        assertThat(calls.get()).isZero();
    }

    @Test
    void streamServerRejectionsAreMappedToGenericErrors() throws Exception {
        responseStatus = 403;
        offer(OFFER_SDP, issueTicket())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Playback ticket already used"));

        responseStatus = 401;
        offer(OFFER_SDP, issueTicket()).andExpect(status().isUnauthorized());

        responseStatus = 400;
        offer(OFFER_SDP, issueTicket()).andExpect(status().isBadRequest());

        // El texto del Streaming Server nunca llega al cliente
        responseStatus = 500;
        offer(OFFER_SDP, issueTicket())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Streaming service unavailable"));
    }

    @Test
    void slowStreamServerTimesOutWith502() throws Exception {
        responseDelayMs = 2000;

        offer(OFFER_SDP, issueTicket()).andExpect(status().isBadGateway());
    }

    @Test
    void invalidBodiesAreRejectedBeforeForwarding() throws Exception {
        String ticket = issueTicket();

        offer("", ticket).andExpect(status().isBadRequest());
        offer("x".repeat(16385), ticket).andExpect(status().isBadRequest());
        offer(OFFER_SDP, "").andExpect(status().isBadRequest());

        assertThat(calls.get()).isZero();
    }

    private ResultActions offer(String sdp, String ticket) throws Exception {
        String body = "{\"sdp\":%s,\"ticket\":%s}".formatted(json(sdp), json(ticket));
        return mockMvc.perform(post("/api/stream/offer").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String issueTicket() throws Exception {
        String json = mockMvc.perform(post("/api/videos").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Video\",\"visibility\":\"public\"}"))
                .andReturn().getResponse().getContentAsString();
        UUID videoId = UUID.fromString(JsonPath.read(json, "$.id"));
        jdbcTemplate.update("UPDATE videos SET status = 'ready' WHERE id = ?", videoId);

        String play = mockMvc.perform(get("/api/videos/{id}/play", videoId).cookie(owner))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(play, "$.ticket");
    }

    private static JWTClaimsSet claims(String audience, Instant expiresAt) {
        return new JWTClaimsSet.Builder()
                .subject("anonymous")
                .claim("vid", UUID.randomUUID().toString())
                .jwtID(UUID.randomUUID().toString())
                .issuer("youbeam-core")
                .audience(audience)
                .issueTime(new Date())
                .expirationTime(Date.from(expiresAt))
                .build();
    }

    private static ECKey attackerKey() throws Exception {
        KeyPair keyPair = TestKeys.generate("secp256r1");
        return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                .privateKey((ECPrivateKey) keyPair.getPrivate())
                .build();
    }

    private static String sign(ECKey key, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), claims);
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }

    private static String json(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}
