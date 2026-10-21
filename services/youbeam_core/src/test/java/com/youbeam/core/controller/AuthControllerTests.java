package com.youbeam.core.controller;

import com.youbeam.core.TestcontainersConfiguration;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTests {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerStoresArgon2idHashAndNeverReturnsIt() throws Exception {
        String username = uniqueUsername();

        register(username, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(content().string(not(containsString("argon2"))))
                .andExpect(content().string(not(containsString(PASSWORD))));

        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = ?", String.class, username);
        assertThat(hash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
    }

    @Test
    void registerWithExistingUsernameReturnsConflict() throws Exception {
        String username = uniqueUsername();
        register(username, PASSWORD).andExpect(status().isCreated());

        register(username, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username or email already registered"));
    }

    @Test
    void registerWithShortPasswordReturnsBadRequest() throws Exception {
        register(uniqueUsername(), "short").andExpect(status().isBadRequest());
    }

    @Test
    void loginWithUsernameOrEmailSucceeds() throws Exception {
        String username = uniqueUsername();
        register(username, PASSWORD).andExpect(status().isCreated());

        login(username, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
        login(username + "@youbeam.test", PASSWORD).andExpect(status().isOk());

        Integer logins = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE username = ? AND last_login_at IS NOT NULL",
                Integer.class, username);
        assertThat(logins).isOne();
    }

    @Test
    void loginOpensSessionWithoutExposingTheToken() throws Exception {
        String username = uniqueUsername();
        register(username, PASSWORD).andExpect(status().isCreated());

        login(username, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").doesNotExist());

        Integer sessions = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sessions s JOIN users u ON u.id = s.user_id "
                        + "WHERE u.username = ? AND s.status = 'active'", Integer.class, username);
        assertThat(sessions).isOne();
    }

    @Test
    void failedLoginDoesNotOpenSession() throws Exception {
        String username = uniqueUsername();
        register(username, PASSWORD).andExpect(status().isCreated());

        login(username, "wrong-password-123").andExpect(status().isUnauthorized());

        Integer sessions = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sessions s JOIN users u ON u.id = s.user_id WHERE u.username = ?",
                Integer.class, username);
        assertThat(sessions).isZero();
    }

    @Test
    void wrongPasswordAndUnknownUserReturnTheSameError() throws Exception {
        String username = uniqueUsername();
        register(username, PASSWORD).andExpect(status().isCreated());

        String wrongPassword = login(username, "wrong-password-123")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownUser = login("nobody-" + username, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    void suspendedUserCannotLogin() throws Exception {
        String username = uniqueUsername();
        register(username, PASSWORD).andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE users SET status = 'suspended' WHERE username = ?", username);

        login(username, PASSWORD).andExpect(status().isUnauthorized());
    }

    private ResultActions register(String username, String password) throws Exception {
        String body = """
                {"username":"%s","email":"%s@youbeam.test","password":"%s"}
                """.formatted(username, username, password);
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String login, String password) throws Exception {
        String body = """
                {"login":"%s","password":"%s"}
                """.formatted(login, password);
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String uniqueUsername() {
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }
}
