package com.youbeam.core.repository;

import com.youbeam.core.entity.VideoKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Acceso a la tabla {@code video_keys}.
 */
public interface VideoKeyRepository extends JpaRepository<VideoKey, UUID> {
}
