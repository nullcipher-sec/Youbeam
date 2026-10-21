package com.youbeam.core.util;

import com.youbeam.core.enums.UserStatus;
import jakarta.persistence.Converter;

/**
 * Convierte {@link UserStatus} a los valores en minúsculas del ENUM {@code user_status}.
 */
@Converter
public class UserStatusConverter extends LowercaseEnumConverter<UserStatus> {

    public UserStatusConverter() {
        super(UserStatus.class);
    }
}
