package com.youbeam.core.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Oferta WebRTC del navegador junto con su ticket de reproducción.
 * Sin {@code toString()} para que el ticket nunca acabe en un log.
 */
@Getter
@Setter
public class StreamOfferRequest {

    // Una oferta SDP real ocupa unos pocos KB; el límite evita reenviar cuerpos gigantes
    @NotBlank
    @Size(max = 16384)
    private String sdp;

    @NotBlank
    @Size(max = 4096)
    private String ticket;
}
