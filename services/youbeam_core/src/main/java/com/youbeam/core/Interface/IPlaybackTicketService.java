package com.youbeam.core.Interface;

import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.response.PlaybackTicketResponse;

import java.util.UUID;

/**
 * Emisión de tickets de reproducción (JWT ES256 de vida corta).
 */
public interface IPlaybackTicketService {

    /**
     * @param user usuario de la petición, o {@code null} si no hay sesión
     */
    PlaybackTicketResponse issue(UUID videoId, AuthenticatedUser user) throws ControllerException;

    /**
     * Comprueba firma ES256, emisor, audiencia y caducidad. No comprueba reutilización: eso lo
     * hace el Streaming Server con el {@code jti}.
     */
    boolean isValid(String ticket);
}
