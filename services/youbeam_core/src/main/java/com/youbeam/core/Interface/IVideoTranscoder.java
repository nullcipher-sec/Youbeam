package com.youbeam.core.Interface;

import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.util.UploadWorkspace;

/**
 * Convierte el archivo subido a los formatos que el Streaming Server envía por WebRTC.
 */
public interface IVideoTranscoder {

    /**
     * Lee {@link UploadWorkspace#original()} y escribe {@link UploadWorkspace#video()} (VP8 en IVF)
     * y {@link UploadWorkspace#audio()} (Opus en Ogg). Si el original no tiene audio se genera
     * una pista de silencio, porque el Streaming Server espera siempre las dos.
     *
     * @return duración del video en segundos
     */
    int transcode(UploadWorkspace workspace) throws ControllerException;
}
