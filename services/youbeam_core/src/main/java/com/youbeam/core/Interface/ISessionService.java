package com.youbeam.core.Interface;

import com.youbeam.core.entity.User;

import java.util.Optional;

/**
 * Creación y validación de sesiones basadas en token.
 */
public interface ISessionService {

    /**
     * Crea una sesión para el usuario y devuelve el token en claro, que solo existe en esta respuesta.
     */
    String create(User user, String ipAddress, String userAgent);

    /**
     * Devuelve el usuario dueño del token si la sesión está activa, no ha expirado y la cuenta está activa.
     */
    Optional<User> findActiveUser(String token);
}
