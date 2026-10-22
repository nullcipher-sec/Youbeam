package com.youbeam.core.service;

import com.youbeam.core.Interface.IVideoStorageService;
import com.youbeam.core.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Almacenamiento en MinIO con el SDK oficial. Solo recibe archivos ya cifrados: el bucket nunca
 * ve un video en claro.
 */
@Service
@RequiredArgsConstructor
public class MinioVideoStorageService implements IVideoStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinioVideoStorageService.class);

    // Son datos cifrados: para quien no tiene la DEK no son ni video ni audio
    private static final String ENCRYPTED_CONTENT_TYPE = "application/octet-stream";

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public String bucket() {
        return properties.bucket();
    }

    @Override
    public String videoKey(UUID videoId) {
        return "videos/" + videoId + "/video.enc";
    }

    @Override
    public String audioKey(UUID videoId) {
        return "videos/" + videoId + "/audio.enc";
    }

    @Override
    public void put(String key, Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .stream(in, Files.size(file), -1)
                    .contentType(ENCRYPTED_CONTENT_TYPE)
                    .build());
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Cannot upload object " + key, ex);
        }
    }

    @Override
    public void deleteQuietly(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(properties.bucket()).object(key).build());
        } catch (Exception ex) {
            logger.error("Cannot delete object {} from bucket {}", key, properties.bucket(), ex);
        }
    }
}
