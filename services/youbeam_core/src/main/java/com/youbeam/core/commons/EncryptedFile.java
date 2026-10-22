package com.youbeam.core.commons;

import java.nio.file.Path;

/**
 * Archivo cifrado listo para subir al bucket.
 *
 * @param path           ruta local dentro del workspace de la subida
 * @param sizeBytes      tamaño del archivo cifrado
 * @param checksumSha256 SHA-256 en hexadecimal del archivo cifrado (lo que se guarda en el bucket)
 */
public record EncryptedFile(Path path, long sizeBytes, String checksumSha256) {
}
