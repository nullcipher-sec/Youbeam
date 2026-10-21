package com.youbeam.core.controller;

import com.youbeam.core.Interface.IAuthService;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.request.LoginRequest;
import com.youbeam.core.model.request.RegisterRequest;
import com.youbeam.core.model.response.LoginResult;
import com.youbeam.core.model.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de registro e inicio de sesión.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request)
            throws ControllerException {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) throws ControllerException {
        LoginResult result = authService.login(request, httpRequest.getRemoteAddr(),
                httpRequest.getHeader(HttpHeaders.USER_AGENT));
        // El token de sesión se entregará en una cookie HttpOnly (siguiente tarea), nunca en el cuerpo
        return ResponseEntity.ok(result.user());
    }
}
