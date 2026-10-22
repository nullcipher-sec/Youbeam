package com.youbeam.core.Interface;

import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.response.VideoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Subida del archivo de un video: recepción, transcodificación y cifrado en reposo.
 */
public interface IVideoUploadService {

    /**
     * Solo el dueño puede subir el archivo, y solo mientras el video no esté listo.
     */
    VideoResponse upload(UUID id, MultipartFile file, AuthenticatedUser user) throws ControllerException;
}
