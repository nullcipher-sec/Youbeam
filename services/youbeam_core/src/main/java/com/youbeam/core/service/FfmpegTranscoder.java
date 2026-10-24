package com.youbeam.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youbeam.core.Interface.IVideoTranscoder;
import com.youbeam.core.config.FfmpegProperties;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.util.UploadWorkspace;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Transcodifica con ffmpeg a VP8/IVF y Opus/Ogg, los formatos que el Streaming Server lee con
 * {@code ivfreader} y {@code oggreader} de Pion.
 *
 * ffmpeg procesa un archivo que controla el atacante, así que se endurece su ejecución:
 * <ul>
 *   <li>Se lanza sin shell, con la lista de argumentos: no hay inyección de comandos posible.</li>
 *   <li>Solo se aceptan los demuxers de MP4/MOV y Matroska/WebM, y solo el protocolo {@code file}.
 *       Sin esto, un "video" que en realidad es una lista HLS podría hacer que ffmpeg lea archivos
 *       locales o haga peticiones a la red interna (SSRF).</li>
 *   <li>Cada proceso tiene un tiempo máximo; si lo supera se mata.</li>
 *   <li>Se descartan los metadatos ({@code -map_metadata -1}): los videos de móvil suelen llevar la
 *       ubicación GPS, el modelo del dispositivo, etc.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FfmpegTranscoder implements IVideoTranscoder {

    private static final Logger logger = LoggerFactory.getLogger(FfmpegTranscoder.class);

    // Nombres de demuxer de ffmpeg; coinciden con los tipos MIME de youbeam.upload.allowed-content-types
    private static final String ALLOWED_FORMATS = "mov,matroska";
    private static final String ALLOWED_PROTOCOLS = "file";

    private static final String PROBE_OUTPUT = "probe.json";
    private static final String FFMPEG_LOG = "ffmpeg.log";

    private final FfmpegProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public int transcode(UploadWorkspace workspace) throws ControllerException {
        Probe probe = probe(workspace);
        run(transcodeCommand(workspace, probe), workspace, null);
        if (!isNonEmpty(workspace.video()) || !isNonEmpty(workspace.audio())) {
            throw new ControllerException(ErrorResponses.INVALID_VIDEO_FILE);
        }
        return (int) Math.max(1, Math.round(probe.durationSeconds()));
    }

    /**
     * ffprobe valida que el archivo sea un video real y dice si tiene audio y cuánto dura.
     */
    private Probe probe(UploadWorkspace workspace) throws ControllerException {
        Path output = workspace.directory().resolve(PROBE_OUTPUT);
        List<String> command = List.of(properties.ffprobePath(),
                "-v", "error",
                "-protocol_whitelist", ALLOWED_PROTOCOLS,
                "-format_whitelist", ALLOWED_FORMATS,
                "-show_entries", "format=duration:stream=codec_type",
                "-of", "json",
                "file:" + workspace.original());
        run(command, workspace, output);

        try {
            JsonNode json = objectMapper.readTree(output.toFile());
            boolean hasVideo = false;
            boolean hasAudio = false;
            for (JsonNode stream : json.path("streams")) {
                String type = stream.path("codec_type").asText();
                hasVideo |= "video".equals(type);
                hasAudio |= "audio".equals(type);
            }
            double duration = json.path("format").path("duration").asDouble(0);
            if (!hasVideo || duration <= 0) {
                throw new ControllerException(ErrorResponses.INVALID_VIDEO_FILE);
            }
            return new Probe(hasAudio, duration);
        } catch (IOException ex) {
            throw new ControllerException(ErrorResponses.INVALID_VIDEO_FILE, ex);
        }
    }

    /**
     * Una sola ejecución con dos salidas, para decodificar el original una sola vez.
     * Si no hay audio se genera silencio de la misma duración ({@code anullsrc}).
     */
    private List<String> transcodeCommand(UploadWorkspace workspace, Probe probe) {
        List<String> command = new ArrayList<>(List.of(properties.ffmpegPath(),
                "-nostdin", "-hide_banner", "-loglevel", "error",
                "-protocol_whitelist", ALLOWED_PROTOCOLS,
                "-format_whitelist", ALLOWED_FORMATS,
                "-i", "file:" + workspace.original()));
        if (!probe.hasAudio()) {
            command.addAll(List.of("-f", "lavfi", "-i", "anullsrc=r=48000:cl=stereo"));
        }

        // Video: primera pista de video real (0:V excluye las carátulas incrustadas)
        command.addAll(List.of(
                "-map", "0:V:0", "-map_metadata", "-1",
                "-c:v", "libvpx", "-b:v", properties.videoBitrate(), "-deadline", "realtime",
                "-r", String.valueOf(properties.frameRate()),
                "-f", "ivf", "-y", "file:" + workspace.video()));

        // Audio: una página Ogg cada 20 ms, el ritmo al que el Streaming Server envía las muestras
        command.addAll(List.of("-map", probe.hasAudio() ? "0:a:0" : "1:a:0", "-map_metadata", "-1"));
        if (!probe.hasAudio()) {
            command.addAll(List.of("-t", String.valueOf(probe.durationSeconds())));
        }
        command.addAll(List.of(
                "-c:a", "libopus", "-b:a", "128k", "-ar", "48000", "-ac", "2",
                "-page_duration", "20000",
                "-f", "ogg", "-y", "file:" + workspace.audio()));
        return command;
    }

    /**
     * Ejecuta el proceso con su salida de errores en un archivo del workspace (leerla por una
     * tubería puede bloquear el proceso si se llena). Si falla, se registra solo el código de
     * salida: el log de ffmpeg contiene datos del archivo del usuario.
     *
     * @param stdout archivo para la salida estándar, o {@code null} para descartarla
     */
    private void run(List<String> command, UploadWorkspace workspace, Path stdout) throws ControllerException {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workspace.directory().toFile())
                .redirectError(ProcessBuilder.Redirect.appendTo(workspace.directory().resolve(FFMPEG_LOG).toFile()))
                .redirectOutput(stdout != null
                        ? ProcessBuilder.Redirect.to(stdout.toFile())
                        : ProcessBuilder.Redirect.DISCARD);
        String tool = Path.of(command.getFirst()).getFileName().toString();

        Process process;
        try {
            process = builder.start();
        } catch (IOException ex) {
            // ffmpeg no instalado o no ejecutable: es un fallo del servidor, no del archivo
            throw new ControllerException(ErrorResponses.INTERNAL_SERVER_ERROR, ex);
        }
        try {
            if (!process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                logger.warn("{} timed out after {}", tool, properties.timeout());
                throw new ControllerException(ErrorResponses.INVALID_VIDEO_FILE);
            }
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new ControllerException(ErrorResponses.INTERNAL_SERVER_ERROR, ex);
        }
        if (process.exitValue() != 0) {
            logger.debug("{} failed with exit code {}", tool, process.exitValue());
            throw new ControllerException(ErrorResponses.INVALID_VIDEO_FILE);
        }
    }

    private static boolean isNonEmpty(Path file) {
        try {
            return Files.isRegularFile(file) && Files.size(file) > 0;
        } catch (IOException ex) {
            return false;
        }
    }

    private record Probe(boolean hasAudio, double durationSeconds) {
    }
}
