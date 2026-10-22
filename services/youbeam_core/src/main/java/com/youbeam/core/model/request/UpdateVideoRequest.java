package com.youbeam.core.model.request;

import com.youbeam.core.enums.VideoVisibility;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Edición parcial de un video: solo se cambian los campos presentes (no nulos).
 */
@Getter
@Setter
public class UpdateVideoRequest {

    // Opcional, pero si viene no puede estar vacío ni ser solo espacios
    @Size(max = 200)
    @Pattern(regexp = "(?s).*\\S.*")
    private String title;

    @Size(max = 5000)
    private String description;

    private VideoVisibility visibility;
}
