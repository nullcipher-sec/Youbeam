package com.youbeam.core.util;

import com.youbeam.core.enums.VideoStatus;
import jakarta.persistence.Converter;

/**
 * Convierte {@link VideoStatus} a los valores en minúsculas de su ENUM de PostgreSQL.
 */
@Converter
public class VideoStatusConverter extends LowercaseEnumConverter<VideoStatus> {

    public VideoStatusConverter() {
        super(VideoStatus.class);
    }
}
