package com.youbeam.core.service;

import com.youbeam.core.Interface.IVideoService;
import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.entity.Video;
import com.youbeam.core.enums.VideoVisibility;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.model.request.CreateVideoRequest;
import com.youbeam.core.model.request.UpdateVideoRequest;
import com.youbeam.core.model.response.VideoResponse;
import com.youbeam.core.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CRUD de videos. Toda decisión de acceso pasa por {@link VideoAccessPolicy}.
 */
@Service
@RequiredArgsConstructor
public class VideoService implements IVideoService {

    private final VideoRepository videoRepository;
    private final VideoAccessPolicy accessPolicy;

    @Override
    @Transactional
    public VideoResponse create(CreateVideoRequest request, AuthenticatedUser user) {
        Video video = new Video();
        video.setOwnerId(user.id());
        video.setTitle(request.getTitle().trim());
        video.setDescription(request.getDescription());
        changeVisibility(video, request.getVisibility() != null ? request.getVisibility() : VideoVisibility.PRIVATE);
        return VideoResponse.from(videoRepository.saveAndFlush(video));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoResponse> listOwn(AuthenticatedUser user) {
        return videoRepository.findByOwnerIdOrderByCreatedAtDesc(user.id()).stream()
                .map(VideoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoResponse> catalog(int page, int size) {
        return videoRepository.findPublicCatalog(PageRequest.of(page, size)).stream()
                .map(VideoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VideoResponse get(UUID id, AuthenticatedUser user) throws ControllerException {
        return VideoResponse.from(findViewable(id, user));
    }

    @Override
    @Transactional
    public VideoResponse update(UUID id, UpdateVideoRequest request, AuthenticatedUser user)
            throws ControllerException {
        Video video = findModifiable(id, user);
        if (request.getTitle() != null) {
            video.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        if (request.getVisibility() != null) {
            changeVisibility(video, request.getVisibility());
        }
        return VideoResponse.from(videoRepository.saveAndFlush(video));
    }

    @Override
    @Transactional
    public void delete(UUID id, AuthenticatedUser user) throws ControllerException {
        Video video = findModifiable(id, user);
        video.setDeletedAt(LocalDateTime.now());
    }

    /**
     * La primera vez que un video se hace público se guarda la fecha, que ordena el catálogo.
     */
    private static void changeVisibility(Video video, VideoVisibility visibility) {
        video.setVisibility(visibility);
        if (visibility == VideoVisibility.PUBLIC && video.getPublishedAt() == null) {
            video.setPublishedAt(LocalDateTime.now());
        }
    }

    /**
     * Si el usuario no puede ver el video se responde 404, igual que si no existiera:
     * así no se confirma la existencia de videos privados ajenos.
     */
    private Video findViewable(UUID id, AuthenticatedUser user) throws ControllerException {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ControllerException(ErrorResponses.NOT_FOUND));
        if (!accessPolicy.canView(video, user)) {
            throw new ControllerException(ErrorResponses.NOT_FOUND);
        }
        return video;
    }

    /**
     * Quien puede ver el video pero no es el dueño recibe 403; quien ni siquiera puede verlo, 404.
     */
    private Video findModifiable(UUID id, AuthenticatedUser user) throws ControllerException {
        Video video = findViewable(id, user);
        if (!accessPolicy.canModify(video, user)) {
            throw new ControllerException(ErrorResponses.FORBIDDEN);
        }
        return video;
    }
}
