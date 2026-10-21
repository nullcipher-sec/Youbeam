package com.youbeam.core.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Credenciales de inicio de sesión: {@code login} acepta el nombre de usuario o el email.
 * Sin {@code toString()} para que la contraseña nunca acabe en un log.
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Size(max = 255)
    private String login;

    @NotBlank
    @Size(max = 128)
    private String password;
}
