package com.youbeam.core.model.response;

import com.youbeam.core.entity.Video;
import com.youbeam.core.enums.VideoStatus;
import com.youbeam.core.enums.VideoVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadatos públicos de un video.
 */
public record VideoResponse(UUID id, UUID ownerId, String title, String description,
                            VideoVisibility visibility, VideoStatus status, Integer durationSeconds,
                            LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static VideoResponse from(Video video) {
        return new VideoResponse(video.getId(), video.getOwnerId(), video.getTitle(), video.getDescription(),
                video.getVisibility(), video.getStatus(), video.getDurationSeconds(),
                video.getPublishedAt(), video.getCreatedAt(), video.getUpdatedAt());
    }
}
