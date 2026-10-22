package com.youbeam.core.util;

import com.youbeam.core.enums.AssetType;
import jakarta.persistence.Converter;

/**
 * Convierte {@link AssetType} a los valores en minúsculas de su ENUM de PostgreSQL.
 */
@Converter
public class AssetTypeConverter extends LowercaseEnumConverter<AssetType> {

    public AssetTypeConverter() {
        super(AssetType.class);
    }
}
