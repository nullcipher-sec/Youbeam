package com.youbeam.core.Interface;

import com.youbeam.core.commons.EncryptedMedia;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.util.UploadWorkspace;

import java.util.UUID;

/**
 * Cifrado en reposo de los videos (Streaming AEAD + HPKE).
 */
public interface IVideoEncryptionService {

    /**
     * Cifra {@link UploadWorkspace#video()} y {@link UploadWorkspace#audio()} con una DEK nueva y
     * envuelve esa DEK con la clave pública HPKE del Streaming Server.
     */
    EncryptedMedia encrypt(UUID videoId, UploadWorkspace workspace) throws ControllerException;
}
