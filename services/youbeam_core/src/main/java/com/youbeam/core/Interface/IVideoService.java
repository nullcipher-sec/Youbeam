package com.youbeam.core.Interface;

import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.request.CreateVideoRequest;
import com.youbeam.core.model.request.UpdateVideoRequest;
import com.youbeam.core.model.response.VideoResponse;

import java.util.List;
import java.util.UUID;

/**
 * Gestión de los metadatos de videos.
 */
public interface IVideoService {

    VideoResponse create(CreateVideoRequest request, AuthenticatedUser user);

    List<VideoResponse> listOwn(AuthenticatedUser user);

    /**
     * Catálogo público: solo videos {@code public} y listos, del más reciente al más antiguo.
     */
    List<VideoResponse> catalog(int page, int size);

    /**
     * @param user usuario de la petición, o {@code null} si no hay sesión
     */
    VideoResponse get(UUID id, AuthenticatedUser user) throws ControllerException;

    VideoResponse update(UUID id, UpdateVideoRequest request, AuthenticatedUser user) throws ControllerException;

    void delete(UUID id, AuthenticatedUser user) throws ControllerException;
}
