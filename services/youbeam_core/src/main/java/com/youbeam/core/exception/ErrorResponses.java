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

    public static final ErrorResponse VIDEO_ALREADY_UPLOADED = new ErrorResponse(HttpStatus.CONFLICT,
            "Video file already uploaded");

    public static final ErrorResponse UNSUPPORTED_VIDEO_TYPE = new ErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported video format");

    public static final ErrorResponse UPLOAD_IN_PROGRESS = new ErrorResponse(HttpStatus.CONFLICT,
            "An upload for this video is already in progress");

    public static final ErrorResponse INVALID_VIDEO_FILE = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY,
            "Video file could not be processed");

    public static final ErrorResponse INVALID_TICKET = new ErrorResponse(HttpStatus.UNAUTHORIZED,
            "Invalid or expired playback ticket");

    public static final ErrorResponse TICKET_ALREADY_USED = new ErrorResponse(HttpStatus.FORBIDDEN,
            "Playback ticket already used");

    public static final ErrorResponse STREAM_UNAVAILABLE = new ErrorResponse(HttpStatus.BAD_GATEWAY,
            "Streaming service unavailable");

    private ErrorResponses() {
    }
}
