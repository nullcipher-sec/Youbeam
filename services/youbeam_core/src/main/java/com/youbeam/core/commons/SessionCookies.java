package com.youbeam.core.commons;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Crea, lee y borra la cookie que transporta el token de sesión.
 *
 * <ul>
 *   <li>{@code HttpOnly}: el JavaScript de la página no puede leerla (protege del robo por XSS).</li>
 *   <li>{@code Secure}: solo viaja por HTTPS.</li>
 *   <li>{@code SameSite=Strict}: el navegador no la envía en peticiones iniciadas desde otra web (CSRF).</li>
 *   <li>Prefijo {@code __Host-}: el navegador la rechaza si no es {@code Secure}, con {@code Path=/}
 *       y sin {@code Domain}, así un subdominio no puede sobrescribirla.</li>
 * </ul>
 */
@Component
public class SessionCookies {

    public static final String NAME = "__Host-youbeam_session";

    private final Duration sessionDuration;

    public SessionCookies(@Value("${youbeam.session.duration}") Duration sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    public ResponseCookie create(String token) {
        return build(token, sessionDuration);
    }

    /**
     * Cookie vacía con {@code Max-Age=0}: indica al navegador que la borre.
     */
    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private static ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
