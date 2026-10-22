package com.youbeam.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DEK de un video envuelta con HPKE (tabla {@code video_keys}). Guardarla aquí es seguro: solo la
 * puede abrir quien tenga la clave privada HPKE, que está únicamente en el Streaming Server.
 */
@Getter
@Setter
@Entity
@Table(name = "video_keys")
public class VideoKey {

    /** Algoritmos usados: HPKE para envolver la DEK y Streaming AEAD para los archivos. */
    public static final String ALGORITHM = "HPKE-X25519-SHA256-AES256GCM+STREAM-AES256GCM-HKDF-1MB";

    @Id
    @Column(name = "video_id", nullable = false, updatable = false)
    private UUID videoId;

    @Column(name = "key_id", nullable = false, length = 64)
    private String keyId;

    @Column(nullable = false, length = 100)
    private String algorithm = ALGORITHM;

    @Column(name = "wrapped_dek", nullable = false)
    private byte[] wrappedDek;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
