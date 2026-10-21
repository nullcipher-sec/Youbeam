package com.youbeam.core.util;

import com.youbeam.core.enums.SessionStatus;
import jakarta.persistence.Converter;

/**
 * Convierte {@link SessionStatus} a los valores en minúsculas del ENUM {@code session_status}.
 */
@Converter
public class SessionStatusConverter extends LowercaseEnumConverter<SessionStatus> {

    public SessionStatusConverter() {
        super(SessionStatus.class);
    }
}
