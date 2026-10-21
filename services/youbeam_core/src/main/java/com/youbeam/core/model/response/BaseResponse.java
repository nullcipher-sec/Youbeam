package com.youbeam.core.model.response;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/**
 * Cuerpo genérico de éxito para operaciones que no devuelven datos (crear, editar, borrar).
 */
@Getter
@Setter
public class BaseResponse {

    private HttpStatus statusCode = HttpStatus.OK;
    private Integer code = HttpStatus.OK.value();
    private String msg = "Success";
}
