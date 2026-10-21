package com.youbeam.core.model.response;

import com.youbeam.core.entity.User;

import java.util.UUID;

/**
 * Datos públicos del usuario. Nunca incluye el hash de la contraseña.
 */
public record UserResponse(UUID id, String username, String email, String displayName) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName());
    }
}
