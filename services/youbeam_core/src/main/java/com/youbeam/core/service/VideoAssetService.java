package com.youbeam.core.service;

import com.youbeam.core.Interface.IVideoAssetService;
import com.youbeam.core.Interface.IVideoStorageService;
import com.youbeam.core.commons.EncryptedFile;
import com.youbeam.core.commons.EncryptedMedia;
import com.youbeam.core.entity.Video;
import com.youbeam.core.entity.VideoAsset;
import com.youbeam.core.entity.VideoKey;
import com.youbeam.core.enums.AssetType;
import com.youbeam.core.enums.VideoStatus;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.repository.VideoAssetRepository;
import com.youbeam.core.repository.VideoKeyRepository;
import com.youbeam.core.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Persistencia del resultado de una subida. Todo ocurre en una transacción con la fila del video
 * bloqueada: o queda el video listo con sus archivos y su clave, o no queda nada.
 */
@Service
@RequiredArgsConstructor
public class VideoAssetService implements IVideoAssetService {

    private static final String ENCRYPTED_MIME_TYPE = "application/octet-stream";

    private final VideoRepository videoRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final VideoKeyRepository videoKeyRepository;
    private final IVideoStorageService storageService;

    @Override
    @Transactional(rollbackFor = ControllerException.class)
    public void markReady(UUID videoId, int durationSeconds, EncryptedMedia media, String videoKey, String audioKey)
            throws ControllerException {
        Video video = videoRepository.findByIdForUpdate(videoId)
                .orElseThrow(() -> new ControllerException(ErrorResponses.NOT_FOUND));
        if (video.getStatus() == VideoStatus.READY) {
            throw new ControllerException(ErrorResponses.VIDEO_ALREADY_UPLOADED);
        }

        // Restos de un intento anterior fallido
        videoAssetRepository.deleteByVideoId(videoId);
        videoKeyRepository.findById(videoId).ifPresent(videoKeyRepository::delete);
        videoKeyRepository.flush();

        videoAssetRepository.save(asset(videoId, videoKey, media.video()));
        videoAssetRepository.save(asset(videoId, audioKey, media.audio()));

        VideoKey key = new VideoKey();
        key.setVideoId(videoId);
        key.setKeyId(media.keyId());
        key.setWrappedDek(media.wrappedDek());
        videoKeyRepository.save(key);

        video.setDurationSeconds(durationSeconds);
        video.setStatus(VideoStatus.READY);
    }

    @Override
    @Transactional
    public void markFailed(UUID videoId) {
        videoRepository.findByIdForUpdate(videoId)
                .filter(video -> video.getStatus() != VideoStatus.READY)
                .ifPresent(video -> video.setStatus(VideoStatus.ERROR));
    }

    private VideoAsset asset(UUID videoId, String storageKey, EncryptedFile file) {
        VideoAsset asset = new VideoAsset();
        asset.setVideoId(videoId);
        asset.setAssetType(AssetType.ENCRYPTED_BLOB);
        asset.setStorageBucket(storageService.bucket());
        asset.setStorageKey(storageKey);
        asset.setMimeType(ENCRYPTED_MIME_TYPE);
        asset.setFileSizeBytes(file.sizeBytes());
        asset.setChecksumSha256(file.checksumSha256());
        return asset;
    }
}
