package com.youbeam.core.controller;

import com.youbeam.core.Interface.IStreamSignalingService;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.request.StreamOfferRequest;
import com.youbeam.core.model.response.StreamAnswerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Señalización WebRTC. No exige sesión: la autorización viaja en el ticket de reproducción.
 */
@Tag(name = "Streaming", description = "Señalización WebRTC con el Streaming Server")
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final IStreamSignalingService signalingService;

    @Operation(summary = "Enviar oferta WebRTC",
            description = "Reenvía la oferta SDP y el ticket al Streaming Server y devuelve su answer. "
                    + "401 si el ticket es inválido o caducó, 403 si ya se usó, 502 si el Streaming Server no responde.")
    @PostMapping("/offer")
    public ResponseEntity<StreamAnswerResponse> offer(@Valid @RequestBody StreamOfferRequest request)
            throws ControllerException {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(signalingService.forwardOffer(request));
    }
}
