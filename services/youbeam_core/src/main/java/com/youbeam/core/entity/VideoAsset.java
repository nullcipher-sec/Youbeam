package com.youbeam.core.entity;

import com.youbeam.core.enums.AssetType;
import com.youbeam.core.util.AssetTypeConverter;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Archivo de un video guardado en el bucket (tabla {@code video_assets}). El checksum es del archivo
 * cifrado y permite detectar si alguien lo modificó o lo corrompió en el bucket.
 */
@Getter
@Setter
@Entity
@Table(name = "video_assets")
public class VideoAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "video_id", nullable = false, updatable = false)
    private UUID videoId;

    // El cast es necesario porque la columna es un ENUM de PostgreSQL, no un VARCHAR
    @Convert(converter = AssetTypeConverter.class)
    @ColumnTransformer(write = "?::asset_type")
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "storage_bucket", nullable = false, length = 100)
    private String storageBucket;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
