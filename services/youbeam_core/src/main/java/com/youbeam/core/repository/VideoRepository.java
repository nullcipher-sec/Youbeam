package com.youbeam.core.repository;

import com.youbeam.core.entity.Video;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a la tabla {@code videos}. Los videos borrados lógicamente nunca aparecen.
 */
public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    /**
     * Catálogo público. Es SQL nativo porque PostgreSQL no compara un ENUM con un parámetro de texto;
     * los valores van escritos en la consulta. El SQL nativo no aplica {@code @SQLRestriction}, por eso
     * el filtro {@code deleted_at IS NULL} está explícito.
     */
    @Query(value = """
            SELECT * FROM videos
            WHERE deleted_at IS NULL AND visibility = 'public' AND status = 'ready'
            ORDER BY published_at DESC NULLS LAST, created_at DESC
            """, nativeQuery = true)
    List<Video> findPublicCatalog(Pageable pageable);

    /**
     * Bloquea la fila del video hasta el final de la transacción ({@code SELECT ... FOR UPDATE}),
     * para que dos procesos no lo marquen como listo a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Video v WHERE v.id = :id")
    Optional<Video> findByIdForUpdate(UUID id);
}
