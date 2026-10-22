package com.youbeam.core.repository;

import com.youbeam.core.entity.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

/**
 * Acceso a la tabla {@code video_assets}.
 */
public interface VideoAssetRepository extends JpaRepository<VideoAsset, UUID> {

    @Modifying
    @Query("DELETE FROM VideoAsset a WHERE a.videoId = :videoId")
    void deleteByVideoId(UUID videoId);
}
