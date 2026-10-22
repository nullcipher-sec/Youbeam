package com.youbeam.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Directorio temporal de una única subida. Allí viven el archivo original y los archivos en claro
 * que se generan al procesarlo, así que se crea accesible solo para el usuario del proceso
 * ({@code rwx------}) y se borra entero al cerrarlo, haya ido bien o mal el procesamiento.
 *
 * Los nombres de los archivos son fijos: el nombre que envía el cliente nunca se usa como ruta,
 * así no hay forma de escribir fuera del directorio (path traversal).
 */
public final class UploadWorkspace implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(UploadWorkspace.class);

    private static final String ORIGINAL_FILE = "original";
    private static final String VIDEO_FILE = "video.ivf";
    private static final String AUDIO_FILE = "audio.ogg";
    private static final String ENCRYPTED_VIDEO_FILE = "video.enc";
    private static final String ENCRYPTED_AUDIO_FILE = "audio.enc";

    private final Path directory;

    private UploadWorkspace(Path directory) {
        this.directory = directory;
    }

    public static UploadWorkspace create(Path baseDir) throws IOException {
        return new UploadWorkspace(Files.createTempDirectory(baseDir, "upload-", ownerOnly()));
    }

    /**
     * Crea el directorio base si no existe, también accesible solo para el usuario del proceso.
     */
    public static void ensureBaseDir(Path baseDir) throws IOException {
        if (!Files.isDirectory(baseDir)) {
            Files.createDirectories(baseDir, ownerOnly());
        }
    }

    public Path directory() {
        return directory;
    }

    /**
     * Archivo donde se guarda el video tal como lo envió el cliente.
     */
    public Path original() {
        return directory.resolve(ORIGINAL_FILE);
    }

    /**
     * Video transcodificado a VP8 en contenedor IVF (en claro).
     */
    public Path video() {
        return directory.resolve(VIDEO_FILE);
    }

    /**
     * Audio transcodificado a Opus en contenedor Ogg (en claro).
     */
    public Path audio() {
        return directory.resolve(AUDIO_FILE);
    }

    public Path encryptedVideo() {
        return directory.resolve(ENCRYPTED_VIDEO_FILE);
    }

    public Path encryptedAudio() {
        return directory.resolve(ENCRYPTED_AUDIO_FILE);
    }

    /**
     * Borra en cuanto ya no hacen falta el original y los archivos en claro, sin esperar al
     * cierre: así el video sin cifrar está en disco el menor tiempo posible.
     */
    public void deletePlaintext() {
        deleteQuietly(original());
        deleteQuietly(video());
        deleteQuietly(audio());
    }

    /**
     * Borra el directorio y todo su contenido. No lanza excepciones para no tapar el error
     * original del procesamiento; si algo no se puede borrar, se registra.
     */
    @Override
    public void close() {
        try (Stream<Path> paths = Files.walk(directory)) {
            // Primero los archivos y al final el directorio, que debe estar vacío para borrarse
            paths.sorted(Comparator.reverseOrder()).forEach(UploadWorkspace::deleteQuietly);
        } catch (IOException ex) {
            logger.error("Cannot list upload workspace {} for cleanup", directory, ex);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            logger.error("Cannot delete temporary upload file {}", path, ex);
        }
    }

    // En sistemas sin permisos POSIX (Windows, en desarrollo) se usan los permisos por defecto
    private static FileAttribute<?>[] ownerOnly() {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return new FileAttribute<?>[0];
        }
        return new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))};
    }
}
