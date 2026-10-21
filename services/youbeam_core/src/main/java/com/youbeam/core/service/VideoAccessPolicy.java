package com.youbeam.core.service;

import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.entity.Video;
import com.youbeam.core.enums.VideoStatus;
import com.youbeam.core.enums.VideoVisibility;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso a videos, centralizadas para que todos los endpoints apliquen las mismas.
 *
 * <table>
 *   <caption>Quién puede ver un video</caption>
 *   <tr><th>Visibilidad</th><th>¿En el catálogo?</th><th>¿Quién lo ve?</th></tr>
 *   <tr><td>public</td><td>Sí</td><td>Cualquiera, incluso sin sesión</td></tr>
 *   <tr><td>hidden</td><td>No</td><td>Cualquiera que tenga el enlace (UUID imposible de adivinar)</td></tr>
 *   <tr><td>private</td><td>No</td><td>Solo el dueño</td></tr>
 * </table>
 *
 * Quien no es el dueño solo ve videos {@code ready}: uno a medio procesar aún no tiene archivo cifrado.
 */
@Component
public class VideoAccessPolicy {

    /**
     * @param user usuario de la petición, o {@code null} si no hay sesión
     */
    public boolean canView(Video video, AuthenticatedUser user) {
        if (isOwner(video, user)) {
            return true;
        }
        return video.getStatus() == VideoStatus.READY && video.getVisibility() != VideoVisibility.PRIVATE;
    }

    /**
     * Solo el dueño puede editar o borrar, sea cual sea la visibilidad.
     */
    public boolean canModify(Video video, AuthenticatedUser user) {
        return isOwner(video, user);
    }

    private static boolean isOwner(Video video, AuthenticatedUser user) {
        return user != null && video.getOwnerId().equals(user.id());
    }
}
