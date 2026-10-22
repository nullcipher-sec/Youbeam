package com.youbeam.core.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.youbeam.core.Interface.IPlaybackTicketService;
import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.config.JwtProperties;
import com.youbeam.core.entity.Video;
import com.youbeam.core.enums.VideoStatus;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.model.response.PlaybackTicketResponse;
import com.youbeam.core.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Emite el ticket JWT ES256 que autoriza a reproducir un video durante unos segundos.
 *
 * Claims: {@code sub} (usuario o {@code anonymous}), {@code vid} (video), {@code jti} (id único para
 * que el Streaming Server rechace reutilizaciones), {@code iss}/{@code aud} (quién lo emite y para quién)
 * e {@code iat}/{@code exp}. Lo firma el Core con su clave privada; el Streaming Server solo tiene la
 * pública, así que puede verificar tickets pero no fabricarlos.
 */
@Service
public class PlaybackTicketService implements IPlaybackTicketService {

    static final String ANONYMOUS_SUBJECT = "anonymous";

    private final VideoRepository videoRepository;
    private final VideoAccessPolicy accessPolicy;
    private final JwtProperties properties;
    private final ECKey signingKey;
    private final ECDSASigner signer;
    private final ECDSAVerifier verifier;
    private final Clock clock;

    public PlaybackTicketService(VideoRepository videoRepository, VideoAccessPolicy accessPolicy,
                                 JwtProperties properties, ECKey ticketSigningKey) throws JOSEException {
        this.videoRepository = videoRepository;
        this.accessPolicy = accessPolicy;
        this.properties = properties;
        this.signingKey = ticketSigningKey;
        this.signer = new ECDSASigner(ticketSigningKey);
        this.verifier = new ECDSAVerifier(ticketSigningKey.toPublicJWK());
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional(readOnly = true)
    public PlaybackTicketResponse issue(UUID videoId, AuthenticatedUser user) throws ControllerException {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ControllerException(ErrorResponses.NOT_FOUND));
        if (!accessPolicy.canView(video, user)) {
            throw new ControllerException(ErrorResponses.FORBIDDEN);
        }
        // El dueño puede ver su video mientras se procesa, pero aún no hay nada cifrado que reproducir
        if (video.getStatus() != VideoStatus.READY) {
            throw new ControllerException(ErrorResponses.VIDEO_NOT_READY);
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.ticketTtl());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user != null ? user.id().toString() : ANONYMOUS_SUBJECT)
                .claim("vid", video.getId().toString())
                .jwtID(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .audience(properties.audience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(JOSEObjectType.JWT)
                .keyID(signingKey.getKeyID())
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException ex) {
            throw new ControllerException(ErrorResponses.INTERNAL_SERVER_ERROR, ex);
        }
        return new PlaybackTicketResponse(jwt.serialize(), expiresAt);
    }

    @Override
    public boolean isValid(String ticket) {
        try {
            SignedJWT jwt = SignedJWT.parse(ticket);
            // Se exige ES256 explícitamente: nunca se confía en el algoritmo que diga la cabecera
            if (!JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(verifier)) {
                return false;
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            return claims.getExpirationTime() != null
                    && claims.getExpirationTime().toInstant().isAfter(clock.instant())
                    && properties.issuer().equals(claims.getIssuer())
                    && claims.getAudience().contains(properties.audience());
        } catch (ParseException | JOSEException ex) {
            return false;
        }
    }
}
