package com.youbeam.core.service;

import com.youbeam.core.commons.EncryptedMedia;
import com.youbeam.core.config.HpkeKeyConfig;
import com.youbeam.core.config.HpkeProperties;
import com.youbeam.core.support.StreamingServerCrypto;
import com.youbeam.core.support.TestHpkeKeys;
import com.youbeam.core.util.UploadWorkspace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static com.youbeam.core.support.StreamingServerCrypto.decrypt;
import static com.youbeam.core.support.StreamingServerCrypto.unwrap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba el cifrado descifrando exactamente como el Streaming Server ({@link StreamingServerCrypto}).
 */
class VideoEncryptionServiceTests {

    // Más de un segmento de 1 MB, para cubrir el cifrado por bloques
    private static final int TRACK_SIZE = 2 * 1024 * 1024 + 123;

    @TempDir
    Path baseDir;

    private VideoEncryptionService encryptionService;
    private UploadWorkspace workspace;
    private byte[] videoPlain;
    private byte[] audioPlain;

    @BeforeEach
    void setUp() throws Exception {
        HpkeKeyConfig.HpkePublicKey publicKey = new HpkeKeyConfig().hpkePublicKey(
                new HpkeProperties(List.of(TestHpkeKeys.publicKeyFile().toString())));
        encryptionService = new VideoEncryptionService(publicKey);

        workspace = UploadWorkspace.create(baseDir);
        videoPlain = randomBytes(TRACK_SIZE);
        audioPlain = randomBytes(TRACK_SIZE / 3);
        Files.write(workspace.video(), videoPlain);
        Files.write(workspace.audio(), audioPlain);
    }

    @AfterEach
    void closeWorkspace() {
        workspace.close();
    }

    @Test
    void streamingServerCanDecryptBothTracks() throws Exception {
        UUID videoId = UUID.randomUUID();
        EncryptedMedia media = encryptionService.encrypt(videoId, workspace);

        byte[] dek = unwrap(media.wrappedDek(), videoId.toString());
        assertThat(dek).hasSize(32);
        assertThat(decrypt(dek, media.video().path(), videoId + ":video")).isEqualTo(videoPlain);
        assertThat(decrypt(dek, media.audio().path(), videoId + ":audio")).isEqualTo(audioPlain);
    }

    @Test
    void ciphertextIsNotThePlaintextAndChecksumMatches() throws Exception {
        EncryptedMedia media = encryptionService.encrypt(UUID.randomUUID(), workspace);
        byte[] encrypted = Files.readAllBytes(media.video().path());

        assertThat(encrypted).isNotEqualTo(videoPlain);
        assertThat(media.video().sizeBytes()).isEqualTo(encrypted.length);
        assertThat(media.video().checksumSha256())
                .isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encrypted)));
    }

    // Ataque 6 de la guía: copiar el wrapped_dek de otro video no sirve
    @Test
    void wrappedDekIsBoundToItsVideo() throws Exception {
        EncryptedMedia media = encryptionService.encrypt(UUID.randomUUID(), workspace);

        assertThatThrownBy(() -> unwrap(media.wrappedDek(), UUID.randomUUID().toString()))
                .isInstanceOf(GeneralSecurityException.class);
    }

    // El AAD también impide servir la pista de audio como video, o la de otro video
    @Test
    void tracksCannotBeSwapped() throws Exception {
        UUID videoId = UUID.randomUUID();
        EncryptedMedia media = encryptionService.encrypt(videoId, workspace);
        byte[] dek = unwrap(media.wrappedDek(), videoId.toString());

        assertThatThrownBy(() -> decrypt(dek, media.audio().path(), videoId + ":video")).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> decrypt(dek, media.video().path(), UUID.randomUUID() + ":video"))
                .isInstanceOf(Exception.class);
    }

    // Ataque 5 de la guía: cambiar un byte del archivo cifrado
    @Test
    void tamperedCiphertextIsRejected() throws Exception {
        UUID videoId = UUID.randomUUID();
        EncryptedMedia media = encryptionService.encrypt(videoId, workspace);
        byte[] dek = unwrap(media.wrappedDek(), videoId.toString());

        byte[] encrypted = Files.readAllBytes(media.video().path());
        encrypted[encrypted.length / 2] ^= 0x01;
        Files.write(media.video().path(), encrypted);

        assertThatThrownBy(() -> decrypt(dek, media.video().path(), videoId + ":video")).isInstanceOf(Exception.class);
    }

    @Test
    void everyVideoGetsItsOwnDek() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        byte[] firstDek = unwrap(encryptionService.encrypt(first, workspace).wrappedDek(), first.toString());
        byte[] secondDek = unwrap(encryptionService.encrypt(second, workspace).wrappedDek(), second.toString());

        assertThat(firstDek).isNotEqualTo(secondDek);
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
