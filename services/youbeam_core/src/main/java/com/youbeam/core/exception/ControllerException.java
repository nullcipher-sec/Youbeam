package com.youbeam.core.exception;

import lombok.Getter;

/**
 * Error de negocio controlado (petición inválida, no autenticado, prohibido...).
 *
 * No genera stack trace a propósito: son errores esperados, así que construir y
 * registrar la traza de cada uno solo agrega carga de CPU y de logs.
 */
@Getter
public class ControllerException extends Exception {

    private final ErrorResponse errorResponse;

    public ControllerException(ErrorResponse errorResponse) {
        super(errorResponse.getMessage(), null, false, false);
        this.errorResponse = errorResponse;
    }

    public ControllerException(ErrorResponse errorResponse, Throwable cause) {
        super(errorResponse.getMessage(), cause, false, false);
        this.errorResponse = errorResponse;
    }
}
