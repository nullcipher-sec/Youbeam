package com.youbeam.core.enums;

/**
 * Estado de una sesión (tipo {@code session_status} de PostgreSQL).
 */
public enum SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}
