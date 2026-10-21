package com.youbeam.core.service;

import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.entity.Video;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso a videos, centralizadas para que todos los endpoints apliquen las mismas.
 */
@Component
public class VideoAccessPolicy {

    /**
     * Por ahora solo el dueño puede ver un video; las reglas de {@code public}/{@code hidden}
     * se añaden en la tarea de visibilidad.
     */
    public boolean canView(Video video, AuthenticatedUser user) {
        return isOwner(video, user);
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
