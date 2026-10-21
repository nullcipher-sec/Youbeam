package com.youbeam.core.service;

import com.youbeam.core.Interface.IPlaybackTicketService;
import com.youbeam.core.Interface.IStreamSignalingService;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.model.request.StreamOfferRequest;
import com.youbeam.core.model.response.StreamAnswerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Reenvía la oferta WebRTC al Streaming Server y devuelve su respuesta.
 *
 * El Core verifica antes la firma y la caducidad del ticket (defensa en profundidad): así no actúa
 * de proxy para tickets falsos o vencidos. El anti-replay ({@code jti}) lo aplica el Streaming
 * Server, que es quien consume el ticket.
 */
@Service
public class StreamSignalingService implements IStreamSignalingService {

    private static final Logger logger = LoggerFactory.getLogger(StreamSignalingService.class);

    private final IPlaybackTicketService ticketService;
    private final RestClient streamRestClient;

    public StreamSignalingService(IPlaybackTicketService ticketService, RestClient streamRestClient) {
        this.ticketService = ticketService;
        this.streamRestClient = streamRestClient;
    }

    @Override
    public StreamAnswerResponse forwardOffer(StreamOfferRequest request) throws ControllerException {
        if (!ticketService.isValid(request.getTicket())) {
            throw new ControllerException(ErrorResponses.INVALID_TICKET);
        }

        StreamAnswerResponse answer;
        try {
            answer = streamRestClient.post()
                    .uri("/offer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("sdp", request.getSdp(), "ticket", request.getTicket()))
                    .retrieve()
                    .body(StreamAnswerResponse.class);
        } catch (RestClientResponseException ex) {
            // Sin la excepción como causa: incluye el cuerpo de la respuesta del Streaming Server,
            // que no debe acabar en los logs del Core
            throw new ControllerException(mapStreamError(ex.getStatusCode().value()));
        } catch (ResourceAccessException ex) {
            // Conexión rechazada o timeout: no se registra el cuerpo, solo el tipo de fallo
            logger.warn("Streaming Server unreachable: {}", ex.getClass().getSimpleName());
            throw new ControllerException(ErrorResponses.STREAM_UNAVAILABLE, ex);
        } catch (RestClientException ex) {
            throw new ControllerException(ErrorResponses.STREAM_UNAVAILABLE, ex);
        }

        if (answer == null || answer.sdp() == null || answer.sdp().isBlank()) {
            throw new ControllerException(ErrorResponses.STREAM_UNAVAILABLE);
        }
        return answer;
    }

    /**
     * Los errores del Streaming Server se traducen a mensajes del catálogo; su texto nunca llega al cliente.
     */
    private static com.youbeam.core.exception.ErrorResponse mapStreamError(int status) {
        if (status == HttpStatus.BAD_REQUEST.value()) {
            return ErrorResponses.BAD_REQUEST;
        }
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            return ErrorResponses.INVALID_TICKET;
        }
        if (status == HttpStatus.FORBIDDEN.value()) {
            return ErrorResponses.TICKET_ALREADY_USED;
        }
        logger.warn("Streaming Server answered with status {}", status);
        return ErrorResponses.STREAM_UNAVAILABLE;
    }
}
