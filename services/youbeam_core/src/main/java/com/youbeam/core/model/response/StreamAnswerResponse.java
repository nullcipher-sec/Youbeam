package com.youbeam.core.model.response;

/**
 * Respuesta SDP del Streaming Server ({@code type} es siempre {@code answer}).
 */
public record StreamAnswerResponse(String type, String sdp) {
}
