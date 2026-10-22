package com.youbeam.core.util;

import com.youbeam.core.enums.VideoVisibility;
import jakarta.persistence.Converter;

/**
 * Convierte {@link VideoVisibility} a los valores en minúsculas de su ENUM de PostgreSQL.
 */
@Converter
public class VideoVisibilityConverter extends LowercaseEnumConverter<VideoVisibility> {

    public VideoVisibilityConverter() {
        super(VideoVisibility.class);
    }
}
