package com.youbeam.core.repository;

import com.youbeam.core.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Acceso a la tabla {@code videos}. Los videos borrados lógicamente nunca aparecen.
 */
public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
