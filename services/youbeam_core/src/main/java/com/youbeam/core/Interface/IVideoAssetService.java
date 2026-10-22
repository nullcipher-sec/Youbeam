package com.youbeam.core.Interface;

import com.youbeam.core.commons.EncryptedMedia;
import com.youbeam.core.exception.ControllerException;

import java.util.UUID;

/**
 * Registro en la base de datos del resultado de procesar un video.
 */
public interface IVideoAssetService {

    /**
     * En una sola transacción guarda los {@code video_assets}, la {@code video_keys} y deja el video
     * en {@code ready}. Si el video ya estaba listo (otra subida terminó antes) no cambia nada y
     * responde 409.
     */
    void markReady(UUID videoId, int durationSeconds, EncryptedMedia media, String videoKey, String audioKey)
            throws ControllerException;

    /**
     * Deja el video en {@code error} para que el dueño pueda volver a subirlo.
     */
    void markFailed(UUID videoId);
}
