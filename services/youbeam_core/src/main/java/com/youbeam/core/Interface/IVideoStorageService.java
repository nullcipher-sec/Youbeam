package com.youbeam.core.Interface;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Bucket donde se guardan los archivos cifrados de los videos.
 */
public interface IVideoStorageService {

    String bucket();

    /**
     * Clave del objeto de video: {@code videos/{id}/video.enc}, la que lee el Streaming Server.
     */
    String videoKey(UUID videoId);

    /**
     * Clave del objeto de audio: {@code videos/{id}/audio.enc}.
     */
    String audioKey(UUID videoId);

    void put(String key, Path file) throws IOException;

    /**
     * Borra el objeto si existe. No lanza excepciones: se usa para deshacer subidas fallidas.
     */
    void deleteQuietly(String key);
}
