package com.youbeam.core.controller;

import com.youbeam.core.Interface.IAuthService;
import com.youbeam.core.Interface.ISessionService;
import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.commons.SessionCookies;
import com.youbeam.core.config.OpenApiConfig;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.request.LoginRequest;
import com.youbeam.core.model.request.RegisterRequest;
import com.youbeam.core.model.response.LoginResult;
import com.youbeam.core.model.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro, inicio y cierre de sesión. El token de sesión solo viaja en una cookie HttpOnly.
 */
@Tag(name = "Auth", description = "Registration, login and session management")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final ISessionService sessionService;
    private final SessionCookies sessionCookies;

    @Operation(summary = "Register a new user", description = "Password is stored as an Argon2id hash.")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request)
            throws ControllerException {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Log in", description = "Sets the HttpOnly session cookie on success.")
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) throws ControllerException {
        LoginResult result = authService.login(request, httpRequest.getRemoteAddr(),
                httpRequest.getHeader(HttpHeaders.USER_AGENT));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.create(result.sessionToken()).toString())
                .body(result.user());
    }

    @Operation(summary = "Current user", security = @SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME))
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(AuthenticatedUser user) {
        return ResponseEntity.ok(user.toUserResponse());
    }

    /**
     * Revoca la sesión en la base de datos y borra la cookie. Responde 204 aunque no hubiera
     * sesión, para no revelar si el token era válido.
     */
    @Operation(summary = "Log out", description = "Revokes the session and clears the cookie.",
            security = @SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        sessionCookies.read(httpRequest).ifPresent(sessionService::revoke);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.clear().toString())
                .build();
    }
}
