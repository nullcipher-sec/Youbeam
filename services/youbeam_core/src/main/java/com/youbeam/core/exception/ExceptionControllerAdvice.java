package com.youbeam.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones. Todo error sale de la API como un {@link ErrorResponse};
 * las excepciones inesperadas se registran en el servidor y se responden con un 500 genérico.
 */
@RestControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler(ControllerException.class)
    public ResponseEntity<ErrorResponse> handleControllerException(ControllerException ex) {
        ErrorResponse error = ex.getErrorResponse();
        if (error.getStatus().is5xxServerError()) {
            logger.error("ControllerException handled. status={}, code={}, message={}",
                    error.getStatus().value(), error.getCode(), error.getMessage(), ex.getCause());
        } else {
            logger.debug("ControllerException handled. status={}, code={}, message={}",
                    error.getStatus().value(), error.getCode(), error.getMessage());
        }
        return toResponse(error);
    }

    // Solo se registran el campo y la regla que fallaron: el valor rechazado puede ser una contraseña
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(MethodArgumentNotValidException ex) {
        if (logger.isDebugEnabled()) {
            String fields = ex.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ":" + error.getCode())
                    .collect(Collectors.joining(", "));
            logger.debug("Invalid request fields: {}", fields);
        }
        return toResponse(ErrorResponses.BAD_REQUEST);
    }

    // El mensaje de Jackson puede incluir fragmentos del cuerpo, así que solo se registra el tipo
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        logger.debug("Unreadable request body: {}", ex.getClass().getSimpleName());
        return toResponse(ErrorResponses.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return toResponse(ErrorResponses.NOT_FOUND);
    }

    // Parámetro con tipo incorrecto, p. ej. un id que no es UUID: es culpa del cliente, no un 500
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        logger.debug("Invalid request parameter: {}", ex.getName());
        return toResponse(ErrorResponses.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Los errores de Spring MVC (405, 415, 406...) traen su propio código 4xx; se respeta con
        // un mensaje genérico en lugar de convertirlos en un 500
        if (ex instanceof org.springframework.web.ErrorResponse springError
                && springError.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            logger.debug("Client error {}: {}", status.value(), ex.getClass().getSimpleName());
            return toResponse(new ErrorResponse(status, status.getReasonPhrase()));
        }
        logger.error("Unexpected error", ex);
        return toResponse(ErrorResponses.INTERNAL_SERVER_ERROR);
    }

    private static ResponseEntity<ErrorResponse> toResponse(ErrorResponse error) {
        return ResponseEntity.status(error.getStatus()).body(error);
    }
}
