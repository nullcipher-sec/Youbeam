package com.youbeam.core.commons;

/**
 * Resultado de cifrar un video: las dos pistas cifradas y su DEK envuelta con HPKE.
 *
 * @param video      pista de video cifrada ({@code video.enc})
 * @param audio      pista de audio cifrada ({@code audio.enc})
 * @param wrappedDek DEK cifrada con la clave pública del Streaming Server; solo él puede abrirla
 * @param keyId      id de la clave pública HPKE usada
 */
public record EncryptedMedia(EncryptedFile video, EncryptedFile audio, byte[] wrappedDek, String keyId) {
}
