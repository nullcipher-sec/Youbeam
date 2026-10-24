package com.youbeam.core.controller;

import com.jayway.jsonpath.JsonPath;
import com.youbeam.core.TestcontainersConfiguration;
import com.youbeam.core.config.MinioProperties;
import com.youbeam.core.config.UploadProperties;
import com.youbeam.core.support.TestSessions;
import com.youbeam.core.support.TestVideos;
import io.minio.GetBucketPolicyArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.youbeam.core.support.StreamingServerCrypto.decrypt;
import static com.youbeam.core.support.StreamingServerCrypto.unwrap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VideoUploadTests {

    private static final byte[] NOT_A_VIDEO = "not really a video".getBytes(StandardCharsets.UTF_8);

    private static byte[] sampleVideo;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UploadProperties uploadProperties;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    private Cookie owner;
    private Cookie stranger;

    @BeforeAll
    static void generateSampleVideo() throws Exception {
        if (TestVideos.ffmpegAvailable()) {
            sampleVideo = TestVideos.mp4Bytes(true);
        }
    }

    @BeforeEach
    void createUsers() throws Exception {
        owner = TestSessions.registerAndLogin(mockMvc);
        stranger = TestSessions.registerAndLogin(mockMvc);
    }

    @Test
    void uploadedVideoIsEncryptedStoredAndReady() throws Exception {
        assumeFfmpeg();
        String id = createVideo(owner, "{\"title\":\"Upload\"}");

        upload(owner, id, file("video/mp4", sampleVideo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.durationSeconds").value(1));

        List<Map<String, Object>> assets = jdbcTemplate.queryForList(
                "SELECT asset_type::text AS type, storage_bucket, storage_key, mime_type, file_size_bytes, checksum_sha256 "
                        + "FROM video_assets WHERE video_id = ?::uuid ORDER BY storage_key DESC", id);
        assertThat(assets).extracting(row -> row.get("storage_key"))
                .containsExactly("videos/" + id + "/video.enc", "videos/" + id + "/audio.enc");
        assertThat(assets).allSatisfy(row -> {
            assertThat(row.get("type")).isEqualTo("encrypted_blob");
            assertThat(row.get("storage_bucket")).isEqualTo("videos");
            assertThat(row.get("mime_type")).isEqualTo("application/octet-stream");
        });

        // El checksum guardado es el del objeto que hay en el bucket
        for (Map<String, Object> asset : assets) {
            byte[] object = download((String) asset.get("storage_key"));
            assertThat(((Number) asset.get("file_size_bytes")).longValue()).isEqualTo(object.length);
            assertThat(asset.get("checksum_sha256"))
                    .isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(object)));
        }

        // Lo que haría el Streaming Server: abrir la DEK con HPKE y descifrar las pistas
        Map<String, Object> key = jdbcTemplate.queryForMap(
                "SELECT key_id, algorithm, wrapped_dek FROM video_keys WHERE video_id = ?::uuid", id);
        assertThat(key.get("algorithm")).isEqualTo("HPKE-X25519-SHA256-AES256GCM+STREAM-AES256GCM-HKDF-1MB");
        byte[] dek = unwrap((byte[]) key.get("wrapped_dek"), id);
        byte[] ivf = decrypt(dek, download("videos/" + id + "/video.enc"), id + ":video");
        byte[] ogg = decrypt(dek, download("videos/" + id + "/audio.enc"), id + ":audio");
        assertThat(new String(ivf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("DKIF");
        assertThat(new String(ogg, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("OggS");

        // En el bucket no hay nada en claro
        assertThat(new String(download("videos/" + id + "/video.enc"), StandardCharsets.ISO_8859_1))
                .doesNotContain("DKIF");
        assertThat(workspaces()).isEmpty();
    }

    @Test
    void bucketIsPrivate() throws Exception {
        assumeFfmpeg();
        String id = createVideo(owner, "{\"title\":\"Private bucket\",\"visibility\":\"public\"}");
        upload(owner, id, file("video/mp4", sampleVideo)).andExpect(status().isOk());

        assertThat(minioClient.getBucketPolicy(GetBucketPolicyArgs.builder().bucket("videos").build())).isEmpty();

        // Sin credenciales MinIO no entrega el objeto, aunque el video sea público
        HttpResponse<Void> anonymous = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(minioProperties.url() + "/videos/videos/" + id + "/video.enc")).build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(anonymous.statusCode()).isEqualTo(403);
    }

    @Test
    void invalidVideoLeavesNothingBehindAndCanBeRetried() throws Exception {
        assumeFfmpeg();
        String id = createVideo(owner, "{\"title\":\"Retry\"}");

        upload(owner, id, file("video/mp4", NOT_A_VIDEO)).andExpect(status().isUnprocessableEntity());

        assertThat(jdbcTemplate.queryForObject("SELECT status::text FROM videos WHERE id = ?::uuid", String.class, id))
                .isEqualTo("error");
        assertThat(count("video_assets", id)).isZero();
        assertThat(count("video_keys", id)).isZero();
        assertThat(objects(id)).isEmpty();
        assertThat(workspaces()).isEmpty();

        upload(owner, id, file("video/mp4", sampleVideo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"));
        assertThat(count("video_assets", id)).isEqualTo(2);
        assertThat(count("video_keys", id)).isEqualTo(1);
    }

    // El nombre del archivo lo elige el cliente y nunca se usa como ruta
    @Test
    void clientFilenameCannotEscapeTheWorkspace() throws Exception {
        assumeFfmpeg();
        String id = createVideo(owner, "{\"title\":\"Traversal\"}");
        MockMultipartFile malicious = new MockMultipartFile("file", "../../escaped.mp4", "video/mp4", sampleVideo);

        upload(owner, id, malicious).andExpect(status().isOk());

        assertThat(uploadProperties.tempDir().getParent().resolve("escaped.mp4")).doesNotExist();
        assertThat(workspaces()).isEmpty();
    }

    @Test
    void onlyTheOwnerCanUpload() throws Exception {
        String privateId = createVideo(owner, "{\"title\":\"Private\"}");

        // Video privado ajeno: 404, no se confirma que exista
        upload(stranger, privateId, file("video/mp4", NOT_A_VIDEO)).andExpect(status().isNotFound());
        upload(owner, UUID.randomUUID().toString(), file("video/mp4", NOT_A_VIDEO)).andExpect(status().isNotFound());
    }

    @Test
    void readyVideoCannotBeReplaced() throws Exception {
        String id = createVideo(owner, "{\"title\":\"Ready\",\"visibility\":\"public\"}");
        jdbcTemplate.update("UPDATE videos SET status = 'ready' WHERE id = ?::uuid", id);

        upload(stranger, id, file("video/mp4", NOT_A_VIDEO)).andExpect(status().isForbidden());
        upload(owner, id, file("video/mp4", NOT_A_VIDEO)).andExpect(status().isConflict());
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        String id = createVideo(owner, "{\"title\":\"Invalid\"}");

        upload(owner, id, file("video/mp4", new byte[0])).andExpect(status().isBadRequest());
        upload(owner, id, file("application/x-msdownload", NOT_A_VIDEO)).andExpect(status().isUnsupportedMediaType());
        upload(owner, id, file("not a media type", NOT_A_VIDEO)).andExpect(status().isUnsupportedMediaType());
        mockMvc.perform(multipart("/api/videos/{id}/upload", id).cookie(owner)).andExpect(status().isBadRequest());

        assertThat(workspaces()).isEmpty();
    }

    @Test
    void uploadRequiresSession() throws Exception {
        mockMvc.perform(multipart("/api/videos/{id}/upload", UUID.randomUUID()).file(file("video/mp4", NOT_A_VIDEO)))
                .andExpect(status().isUnauthorized());
    }

    private static void assumeFfmpeg() {
        assumeTrue(sampleVideo != null, "ffmpeg is not installed");
    }

    private static MockMultipartFile file(String contentType, byte[] content) {
        return new MockMultipartFile("file", "clip.mp4", contentType, content);
    }

    private ResultActions upload(Cookie session, String id, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/videos/{id}/upload", id).file(file).cookie(session));
    }

    private String createVideo(Cookie session, String body) throws Exception {
        String json = mockMvc.perform(post("/api/videos").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private int count(String table, String videoId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE video_id = ?::uuid",
                Integer.class, videoId);
    }

    private byte[] download(String key) throws Exception {
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder().bucket("videos").object(key).build())) {
            return in.readAllBytes();
        }
    }

    private List<String> objects(String videoId) {
        return StreamSupport.stream(minioClient.listObjects(ListObjectsArgs.builder()
                        .bucket("videos").prefix("videos/" + videoId + "/").recursive(true).build()).spliterator(), false)
                .map(result -> {
                    try {
                        return result.get().objectName();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                })
                .toList();
    }

    private List<Path> workspaces() throws Exception {
        try (Stream<Path> paths = Files.list(uploadProperties.tempDir())) {
            return paths.toList();
        }
    }
}
