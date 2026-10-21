package com.youbeam.core.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Estado de procesamiento de un video (tipo {@code video_status} de PostgreSQL).
 */
public enum VideoStatus {
    /** Creado, pendiente de subir, transcodificar y cifrar el archivo. */
    @JsonProperty("processing") PROCESSING,
    /** Archivo cifrado y listo para reproducir. */
    @JsonProperty("ready") READY,
    /** Falló el procesamiento. */
    @JsonProperty("error") ERROR
}
