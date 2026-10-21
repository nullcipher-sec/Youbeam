package com.youbeam.core.entity;

import com.youbeam.core.enums.VideoStatus;
import com.youbeam.core.enums.VideoVisibility;
import com.youbeam.core.util.VideoStatusConverter;
import com.youbeam.core.util.VideoVisibilityConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadatos de un video (tabla {@code videos}). El borrado es lógico: {@code deleted_at} se rellena
 * y {@link SQLRestriction} excluye esas filas de todas las consultas, así que un video borrado no
 * puede filtrarse por olvidar un filtro.
 */
@Getter
@Setter
@Entity
@Table(name = "videos")
@SQLRestriction("deleted_at IS NULL")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // El cast es necesario porque la columna es un ENUM de PostgreSQL, no un VARCHAR
    @Convert(converter = VideoVisibilityConverter.class)
    @ColumnTransformer(write = "?::video_visibility")
    @Column(nullable = false)
    private VideoVisibility visibility = VideoVisibility.PRIVATE;

    @Convert(converter = VideoStatusConverter.class)
    @ColumnTransformer(write = "?::video_status")
    @Column(nullable = false)
    private VideoStatus status = VideoStatus.PROCESSING;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
