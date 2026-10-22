package com.youbeam.core.model.request;

import com.youbeam.core.enums.VideoVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos para crear un video. Si no se indica visibilidad, el video nace privado.
 */
@Getter
@Setter
public class CreateVideoRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    private VideoVisibility visibility;
}
