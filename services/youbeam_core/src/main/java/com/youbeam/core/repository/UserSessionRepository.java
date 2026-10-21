package com.youbeam.core.repository;

import com.youbeam.core.entity.UserSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a la tabla {@code sessions}.
 */
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<UserSession> findByTokenHash(String tokenHash);
}
