package com.youbeam.core.util;

import jakarta.persistence.AttributeConverter;

import java.util.Locale;

/**
 * Base para convertir enums de Java a los ENUM de PostgreSQL, cuyos valores están en minúsculas.
 */
public abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected LowercaseEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public E convertToEntityAttribute(String value) {
        return value == null ? null : Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
    }
}
