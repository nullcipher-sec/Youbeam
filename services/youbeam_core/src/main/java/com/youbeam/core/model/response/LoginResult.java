package com.youbeam.core.model.response;

/**
 * Resultado interno del login: datos del usuario y token de sesión en claro.
 * No se serializa tal cual; el token solo viaja al cliente en una cookie HttpOnly.
 */
public record LoginResult(UserResponse user, String sessionToken) {

    // Se sobrescribe para que el token nunca aparezca en un log
    @Override
    public String toString() {
        return "LoginResult[user=" + user + ", sessionToken=***]";
    }
}
