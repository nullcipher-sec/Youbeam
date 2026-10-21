package com.youbeam.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Cuerpo de error que se devuelve a los clientes de la API. Es inmutable para que
 * las entradas del catálogo {@link ErrorResponses} se compartan sin riesgo entre peticiones.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final HttpStatus status;
    private final Integer code;
    private final String message;

    public ErrorResponse(HttpStatus status, Integer code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(HttpStatus status, String message) {
        this(status, status.value(), message);
    }
}
