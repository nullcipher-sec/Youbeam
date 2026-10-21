package com.youbeam.core.service;

import com.youbeam.core.Interface.ISessionService;
import com.youbeam.core.TestcontainersConfiguration;
import com.youbeam.core.entity.User;
import com.youbeam.core.repository.UserRepository;
import com.youbeam.core.util.SessionTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SessionServiceTests {

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;

    @BeforeEach
    void createUser() {
        String username = "user" + UUID.randomUUID().toString().substring(0, 8);
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(username + "@youbeam.test");
        newUser.setPasswordHash("not-used-in-these-tests");
        user = userRepository.saveAndFlush(newUser);
    }

    @Test
    void tokenHas256BitsAndOnlyItsHashIsStored() {
        String token = sessionService.create(user, "127.0.0.1", "JUnit");

        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM sessions WHERE user_id = ?", String.class, user.getId());
        assertThat(storedHash).hasSize(64).isEqualTo(SessionTokens.hash(token)).isNotEqualTo(token);

        Integer rowsWithPlainToken = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sessions WHERE token_hash = ?", Integer.class, token);
        assertThat(rowsWithPlainToken).isZero();
    }

    @Test
    void sessionStoresStatusExpiryAndClientData() {
        sessionService.create(user, "127.0.0.1", "JUnit");

        var row = jdbcTemplate.queryForMap(
                "SELECT status::text AS status, ip_address, user_agent, "
                        + "expires_at > now() + interval '7 hours' AS long_enough "
                        + "FROM sessions WHERE user_id = ?", user.getId());
        assertThat(row).containsEntry("status", "active")
                .containsEntry("ip_address", "127.0.0.1")
                .containsEntry("user_agent", "JUnit")
                .containsEntry("long_enough", true);
    }

    @Test
    void eachLoginGetsADifferentToken() {
        String first = sessionService.create(user, null, null);
        String second = sessionService.create(user, null, null);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void validTokenResolvesItsUser() {
        String token = sessionService.create(user, null, null);

        assertThat(sessionService.findActiveUser(token)).get()
                .extracting(User::getId).isEqualTo(user.getId());
    }

    @Test
    void unknownOrEmptyTokenIsRejected() {
        assertThat(sessionService.findActiveUser(SessionTokens.generate())).isEmpty();
        assertThat(sessionService.findActiveUser("")).isEmpty();
        assertThat(sessionService.findActiveUser(null)).isEmpty();
    }

    @Test
    void expiredSessionIsRejectedAndMarkedExpired() {
        String token = sessionService.create(user, null, null);
        jdbcTemplate.update("UPDATE sessions SET expires_at = now() - interval '1 minute' WHERE token_hash = ?",
                SessionTokens.hash(token));

        assertThat(sessionService.findActiveUser(token)).isEmpty();

        String status = jdbcTemplate.queryForObject(
                "SELECT status::text FROM sessions WHERE token_hash = ?", String.class, SessionTokens.hash(token));
        assertThat(status).isEqualTo("expired");
    }

    @Test
    void sessionOfSuspendedUserIsRejected() {
        String token = sessionService.create(user, null, null);
        jdbcTemplate.update("UPDATE users SET status = 'suspended' WHERE id = ?", user.getId());

        assertThat(sessionService.findActiveUser(token)).isEmpty();
    }
}
