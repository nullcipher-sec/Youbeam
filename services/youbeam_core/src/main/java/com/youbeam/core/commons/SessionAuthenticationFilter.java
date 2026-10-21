package com.youbeam.core.commons;

import com.youbeam.core.Interface.ISessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee la cookie de sesión de cada petición y, si la sesión es válida, deja el usuario en
 * {@link AuthenticatedUser#REQUEST_ATTRIBUTE}. No rechaza nada por sí mismo: cada endpoint
 * decide si exige sesión declarando un parámetro {@link AuthenticatedUser}.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final ISessionService sessionService;
    private final SessionCookies sessionCookies;

    public SessionAuthenticationFilter(ISessionService sessionService, SessionCookies sessionCookies) {
        this.sessionService = sessionService;
        this.sessionCookies = sessionCookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Sin cookie no se consulta la base de datos
        sessionCookies.read(request)
                .flatMap(sessionService::findActiveUser)
                .map(AuthenticatedUser::from)
                .ifPresent(user -> request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user));

        filterChain.doFilter(request, response);
    }
}
