package com.youbeam.core.service;

import com.youbeam.core.Interface.IVideoAssetService;
import com.youbeam.core.Interface.IVideoEncryptionService;
import com.youbeam.core.Interface.IVideoStorageService;
import com.youbeam.core.Interface.IVideoTranscoder;
import com.youbeam.core.Interface.IVideoUploadService;
import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.commons.EncryptedMedia;
import com.youbeam.core.config.UploadProperties;
import com.youbeam.core.entity.Video;
import com.youbeam.core.enums.VideoStatus;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.model.response.VideoResponse;
import com.youbeam.core.repository.VideoRepository;
import com.youbeam.core.util.UploadWorkspace;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procesa la subida de un video, de principio a fin (ver {@code docs/GUIA-SEGURIDAD.md}, paso 4):
 * <ol>
 *   <li>Recibe el archivo en un directorio temporal propio ({@link UploadWorkspace}).</li>
 *   <li>Lo transcodifica a VP8/IVF y Opus/Ogg con ffmpeg.</li>
 *   <li>Cifra las dos pistas con una DEK nueva y envuelve la DEK con HPKE.</li>
 *   <li>Borra el original y los archivos en claro, y sube los cifrados al bucket.</li>
 *   <li>Guarda {@code video_assets} y {@code video_keys} y deja el video en {@code ready}.</li>
 * </ol>
 * El directorio temporal se borra siempre. Si algo falla, se eliminan del bucket los objetos ya
 * subidos y el video queda en {@code error} para que el dueño lo pueda reintentar.
 *
 * No se abre una transacción aquí: el proceso puede tardar minutos y no debe retener una conexión
 * a la BD. Solo el paso final ({@link IVideoAssetService#markReady}) es transaccional.
 */
@Service
@RequiredArgsConstructor
public class VideoUploadService implements IVideoUploadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadService.class);

    private final VideoRepository videoRepository;
    private final VideoAccessPolicy accessPolicy;
    private final UploadProperties properties;
    private final IVideoTranscoder transcoder;
    private final IVideoEncryptionService encryptionService;
    private final IVideoStorageService storageService;
    private final IVideoAssetService assetService;

    // Videos con una subida en curso. Dos subidas simultáneas del mismo video escribirían los mismos
    // objetos del bucket con DEKs distintas; se asume una sola instancia del Core
    private final Set<UUID> uploadsInProgress = ConcurrentHashMap.newKeySet();

    @Override
    public VideoResponse upload(UUID id, MultipartFile file, AuthenticatedUser user) throws ControllerException {
        checkUploadable(id, user);
        validate(file);
        if (!uploadsInProgress.add(id)) {
            throw new ControllerException(ErrorResponses.UPLOAD_IN_PROGRESS);
        }
        try {
            process(id, file);
        } catch (ControllerException | RuntimeException ex) {
            assetService.markFailed(id);
            throw ex;
        } finally {
            uploadsInProgress.remove(id);
        }
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ControllerException(ErrorResponses.NOT_FOUND));
        return VideoResponse.from(video);
    }

    private void process(UUID id, MultipartFile file) throws ControllerException {
        try (UploadWorkspace workspace = UploadWorkspace.create(properties.tempDir())) {
            file.transferTo(workspace.original());
            // Se registran el id y el tamaño, nunca el nombre del archivo: lo controla el cliente
            logger.info("Upload received. videoId={}, bytes={}", id, file.getSize());

            int durationSeconds = transcoder.transcode(workspace);
            EncryptedMedia media = encryptionService.encrypt(id, workspace);
            workspace.deletePlaintext();

            store(id, durationSeconds, media);
            logger.info("Video encrypted and stored. videoId={}, keyId={}", id, media.keyId());
        } catch (IOException ex) {
            throw new ControllerException(ErrorResponses.INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Sube los objetos cifrados y registra el resultado. Si el registro falla se borran los objetos,
     * salvo que el video ya estuviera listo: entonces esos objetos son los del video bueno.
     */
    private void store(UUID id, int durationSeconds, EncryptedMedia media) throws ControllerException {
        String videoKey = storageService.videoKey(id);
        String audioKey = storageService.audioKey(id);
        try {
            storageService.put(videoKey, media.video().path());
            storageService.put(audioKey, media.audio().path());
            assetService.markReady(id, durationSeconds, media, videoKey, audioKey);
        } catch (IOException ex) {
            removeObjects(videoKey, audioKey);
            throw new ControllerException(ErrorResponses.INTERNAL_SERVER_ERROR, ex);
        } catch (ControllerException ex) {
            if (ex.getErrorResponse() != ErrorResponses.VIDEO_ALREADY_UPLOADED) {
                removeObjects(videoKey, audioKey);
            }
            throw ex;
        } catch (RuntimeException ex) {
            removeObjects(videoKey, audioKey);
            throw ex;
        }
    }

    private void removeObjects(String... keys) {
        for (String key : keys) {
            storageService.deleteQuietly(key);
        }
    }

    /**
     * Mismas reglas que editar: 404 si no puede verlo, 403 si no es el dueño. Un video listo ya
     * tiene su archivo cifrado y no se reemplaza; uno con error sí admite un nuevo intento.
     */
    private void checkUploadable(UUID id, AuthenticatedUser user) throws ControllerException {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ControllerException(ErrorResponses.NOT_FOUND));
        if (!accessPolicy.canView(video, user)) {
            throw new ControllerException(ErrorResponses.NOT_FOUND);
        }
        if (!accessPolicy.canModify(video, user)) {
            throw new ControllerException(ErrorResponses.FORBIDDEN);
        }
        if (video.getStatus() == VideoStatus.READY) {
            throw new ControllerException(ErrorResponses.VIDEO_ALREADY_UPLOADED);
        }
    }

    /**
     * El tipo MIME lo declara el cliente, así que solo filtra errores evidentes; el contenido
     * real lo valida ffmpeg al transcodificar.
     */
    private void validate(MultipartFile file) throws ControllerException {
        if (file.isEmpty()) {
            throw new ControllerException(ErrorResponses.BAD_REQUEST);
        }
        if (!isAllowedContentType(file.getContentType())) {
            throw new ControllerException(ErrorResponses.UNSUPPORTED_VIDEO_TYPE);
        }
    }

    private boolean isAllowedContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            MediaType type = MediaType.parseMediaType(contentType);
            return properties.allowedContentTypes().stream()
                    .map(MediaType::parseMediaType)
                    .anyMatch(allowed -> allowed.equalsTypeAndSubtype(type));
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }
}
