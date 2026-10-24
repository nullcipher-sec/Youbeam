package com.youbeam.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youbeam.core.config.FfmpegProperties;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.support.TestVideos;
import com.youbeam.core.util.UploadWorkspace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FfmpegTranscoderTests {

    private final FfmpegTranscoder transcoder = new FfmpegTranscoder(
            new FfmpegProperties("ffmpeg", "ffprobe", Duration.ofMinutes(1), "2M", 30), new ObjectMapper());

    @TempDir
    Path baseDir;

    private UploadWorkspace workspace;

    @BeforeEach
    void createWorkspace() throws Exception {
        assumeTrue(TestVideos.ffmpegAvailable(), "ffmpeg is not installed");
        workspace = UploadWorkspace.create(baseDir);
    }

    @AfterEach
    void closeWorkspace() {
        if (workspace != null) {
            workspace.close();
        }
    }

    @Test
    void producesVp8IvfAndOpusOggWithoutMetadata() throws Exception {
        TestVideos.mp4(workspace.original(), true);

        int duration = transcoder.transcode(workspace);

        assertThat(duration).isEqualTo(1);
        byte[] video = Files.readAllBytes(workspace.video());
        byte[] audio = Files.readAllBytes(workspace.audio());
        // Cabecera IVF con el códec VP8, y página Ogg con cabecera Opus
        assertThat(new String(video, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("DKIF");
        assertThat(new String(video, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("VP80");
        assertThat(new String(audio, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("OggS");
        assertThat(new String(audio, StandardCharsets.ISO_8859_1)).contains("OpusHead");

        assertThat(new String(video, StandardCharsets.ISO_8859_1)).doesNotContain(TestVideos.SECRET_METADATA);
        assertThat(new String(audio, StandardCharsets.ISO_8859_1)).doesNotContain(TestVideos.SECRET_METADATA);
    }

    // El Streaming Server necesita siempre las dos pistas
    @Test
    void generatesSilentAudioWhenTheOriginalHasNone() throws Exception {
        TestVideos.mp4(workspace.original(), false);

        assertThat(transcoder.transcode(workspace)).isEqualTo(1);
        assertThat(new String(Files.readAllBytes(workspace.audio()), StandardCharsets.ISO_8859_1)).contains("OpusHead");
    }

    @Test
    void rejectsFilesThatAreNotVideos() throws Exception {
        Files.writeString(workspace.original(), "definitely not a video");

        assertInvalid();
    }

    // Una lista HLS disfrazada de video haría que ffmpeg lea archivos locales o pida URLs internas
    @Test
    void rejectsHlsPlaylistsThatReferenceOtherFiles() throws Exception {
        Path secret = baseDir.resolve("secret.txt");
        Files.writeString(secret, "top secret");
        Files.writeString(workspace.original(), """
                #EXTM3U
                #EXT-X-TARGETDURATION:1
                #EXTINF:1,
                file://%s
                #EXT-X-ENDLIST
                """.formatted(secret));

        assertInvalid();
        assertThat(workspace.video()).doesNotExist();
    }

    private void assertInvalid() {
        assertThatThrownBy(() -> transcoder.transcode(workspace))
                .isInstanceOfSatisfying(ControllerException.class,
                        ex -> assertThat(ex.getErrorResponse()).isEqualTo(ErrorResponses.INVALID_VIDEO_FILE));
    }
}
