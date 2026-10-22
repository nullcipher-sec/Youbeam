package com.youbeam.core.enums;

/**
 * Tipo de archivo de un video en el bucket (tipo {@code asset_type} de PostgreSQL).
 */
public enum AssetType {
    ORIGINAL,
    HLS,
    THUMBNAIL,
    /** Pista cifrada con Streaming AEAD; sin la DEK no se puede leer. */
    ENCRYPTED_BLOB
}
