package com.youbeam.core.util;

import com.youbeam.core.enums.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/**
 * Convierte {@link UserStatus} a los valores en minúsculas del ENUM {@code user_status}.
 */
@Converter
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus status) {
        return status == null ? null : status.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public UserStatus convertToEntityAttribute(String value) {
        return value == null ? null : UserStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
