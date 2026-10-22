package com.youbeam.core.model.response;

import java.time.Instant;

/**
 * Ticket firmado que el cliente entrega al Streaming Server para reproducir un video.
 */
public record PlaybackTicketResponse(String ticket, Instant expiresAt) {

    // Se sobrescribe para que el ticket nunca aparezca en un log
    @Override
    public String toString() {
        return "PlaybackTicketResponse[ticket=***, expiresAt=" + expiresAt + "]";
    }
}
