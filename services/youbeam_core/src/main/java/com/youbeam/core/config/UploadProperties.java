package com.youbeam.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;

/**
 * Subida de videos ({@code youbeam.upload.*}). El tamaño máximo se fija aparte, en
 * {@code spring.servlet.multipart.max-file-size}, porque lo aplica el propio servidor web.
 *
 * @param tempDir             directorio base de trabajo; cada subida usa un subdirectorio propio
 * @param allowedContentTypes tipos MIME aceptados; es una primera barrera, ffmpeg valida el contenido real
 */
@ConfigurationProperties(prefix = "youbeam.upload")
public record UploadProperties(Path tempDir, List<String> allowedContentTypes) {
}
