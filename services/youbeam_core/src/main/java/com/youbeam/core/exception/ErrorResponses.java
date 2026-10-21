package com.youbeam.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de errores controlados de la API. Los mensajes son genéricos a propósito:
 * no deben revelar si un usuario, video o sesión existe.
 */
public final class ErrorResponses {

    public static final ErrorResponse INTERNAL_SERVER_ERROR = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal error, please try again later");

    public static final ErrorResponse BAD_REQUEST = new ErrorResponse(HttpStatus.BAD_REQUEST,
            "Invalid request");

    public static final ErrorResponse UNAUTHORIZED = new ErrorResponse(HttpStatus.UNAUTHORIZED,
            "Authentication required");

    public static final ErrorResponse FORBIDDEN = new ErrorResponse(HttpStatus.FORBIDDEN,
            "Access denied");

    public static final ErrorResponse NOT_FOUND = new ErrorResponse(HttpStatus.NOT_FOUND,
            "Resource not found");

    public static final ErrorResponse INVALID_CREDENTIALS = new ErrorResponse(HttpStatus.UNAUTHORIZED,
            "Invalid username or password");

    public static final ErrorResponse USER_ALREADY_EXISTS = new ErrorResponse(HttpStatus.CONFLICT,
            "Username or email already registered");

    public static final ErrorResponse VIDEO_NOT_READY = new ErrorResponse(HttpStatus.CONFLICT,
            "Video is not ready for playback");

    private ErrorResponses() {
    }
}
