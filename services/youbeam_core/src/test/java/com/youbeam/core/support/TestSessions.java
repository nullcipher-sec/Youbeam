package com.youbeam.core.support;

import com.youbeam.core.commons.SessionCookies;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Crea usuarios de prueba y devuelve su cookie de sesión.
 */
public final class TestSessions {

    public static final String PASSWORD = "correct-horse-battery";

    private TestSessions() {
    }

    public static String uniqueUsername() {
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static Cookie registerAndLogin(MockMvc mockMvc) throws Exception {
        String username = uniqueUsername();
        String register = """
                {"username":"%s","email":"%s@youbeam.test","password":"%s"}
                """.formatted(username, username, PASSWORD);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(register));

        String login = """
                {"login":"%s","password":"%s"}
                """.formatted(username, PASSWORD);
        Cookie cookie = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andReturn().getResponse().getCookie(SessionCookies.NAME);
        if (cookie == null) {
            throw new IllegalStateException("Login did not return a session cookie");
        }
        return cookie;
    }
}
