package com.youbeam.core.config;

import com.youbeam.core.util.UploadWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Prepara el directorio de trabajo de las subidas al arrancar. Si no se puede crear, la
 * aplicación no arranca: es mejor fallar al inicio que en la primera subida.
 */
@Configuration
@EnableConfigurationProperties({UploadProperties.class, FfmpegProperties.class})
public class UploadConfig {

    private static final Logger logger = LoggerFactory.getLogger(UploadConfig.class);

    public UploadConfig(UploadProperties properties) {
        try {
            UploadWorkspace.ensureBaseDir(properties.tempDir());
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot create upload temp dir " + properties.tempDir(), ex);
        }
        logger.info("Upload temp dir: {}", properties.tempDir());
    }
}
