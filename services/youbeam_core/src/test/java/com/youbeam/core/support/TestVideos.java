package com.youbeam.core.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Genera videos MP4 reales de 1 segundo con ffmpeg (patrón de prueba y tono), para los tests de subida.
 * Llevan un título en los metadatos para comprobar que el Core los elimina.
 */
public final class TestVideos {

    public static final String SECRET_METADATA = "SECRET-GPS-40.4168-3.7038";

    private TestVideos() {
    }

    public static boolean ffmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static Path mp4(Path target, boolean withAudio) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("ffmpeg", "-nostdin", "-loglevel", "error",
                "-f", "lavfi", "-i", "testsrc=size=160x120:rate=30"));
        if (withAudio) {
            command.addAll(List.of("-f", "lavfi", "-i", "sine=frequency=440:sample_rate=48000"));
        }
        command.addAll(List.of("-t", "1", "-metadata", "title=" + SECRET_METADATA, "-c:v", "mpeg4"));
        if (withAudio) {
            command.addAll(List.of("-c:a", "aac"));
        }
        command.addAll(List.of("-f", "mp4", "-y", target.toString()));

        Process process = new ProcessBuilder(command).inheritIO().start();
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("Cannot generate test video");
        }
        return target;
    }

    public static byte[] mp4Bytes(boolean withAudio) throws IOException, InterruptedException {
        Path file = Files.createTempFile("clip", ".mp4");
        try {
            return Files.readAllBytes(mp4(file, withAudio));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
