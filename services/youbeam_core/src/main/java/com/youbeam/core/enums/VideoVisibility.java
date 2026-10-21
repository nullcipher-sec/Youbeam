package com.youbeam.core.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Visibilidad de un video (tipo {@code video_visibility} de PostgreSQL). En la API viaja en minúsculas.
 */
public enum VideoVisibility {
    /** Aparece en el catálogo y cualquiera puede verlo. */
    @JsonProperty("public") PUBLIC,
    /** No aparece en el catálogo; lo ve quien tenga el enlace (UUID imposible de adivinar). */
    @JsonProperty("hidden") HIDDEN,
    /** Solo lo ve su dueño. */
    @JsonProperty("private") PRIVATE
}
