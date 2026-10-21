package com.youbeam.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex) {
        logger.debug("Invalid request: {}", ex.getMessage());
        return toResponse(ErrorResponses.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return toResponse(ErrorResponses.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        logger.error("Unexpected error", ex);
        return toResponse(ErrorResponses.INTERNAL_SERVER_ERROR);
    }

    private static ResponseEntity<ErrorResponse> toResponse(ErrorResponse error) {
        return ResponseEntity.status(error.getStatus()).body(error);
    }
}
