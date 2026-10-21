package com.youbeam.core.commons;

import com.youbeam.core.entity.User;
import com.youbeam.core.model.response.UserResponse;

import java.util.UUID;

/**
 * Usuario autenticado en la petición actual, resuelto a partir de la cookie de sesión.
 * Se declara como parámetro de un endpoint para exigir sesión; si no la hay se responde 401.
 */
public record AuthenticatedUser(UUID id, String username, String email, String displayName) {

    /** Atributo de la petición donde {@link SessionAuthenticationFilter} deja al usuario. */
    public static final String REQUEST_ATTRIBUTE = AuthenticatedUser.class.getName();

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName());
    }

    public UserResponse toUserResponse() {
        return new UserResponse(id, username, email, displayName);
    }
}
