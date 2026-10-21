package com.youbeam.core.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos de registro. Sin {@code toString()} para que la contraseña nunca acabe en un log.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    // Mínimo 12 caracteres; el máximo evita gastar CPU/memoria hasheando entradas enormes
    @NotBlank
    @Size(min = 12, max = 128)
    private String password;

    @Size(max = 100)
    private String displayName;
}
