package com.youbeam.core.service;

import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKey;
import com.google.crypto.tink.streamingaead.PredefinedStreamingAeadParameters;
import com.youbeam.core.Interface.IVideoEncryptionService;
import com.youbeam.core.commons.EncryptedFile;
import com.youbeam.core.commons.EncryptedMedia;
import com.youbeam.core.config.HpkeKeyConfig.HpkePublicKey;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.util.UploadWorkspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Cifrado híbrido de cada video:
 * <ol>
 *   <li>Se genera una DEK aleatoria solo para este video ({@code AES256_GCM_HKDF_1MB}).</li>
 *   <li>Video y audio se cifran con Streaming AEAD (AES-256-GCM por segmentos de 1 MB), sin cargar
 *       el archivo en memoria. El AAD {@code {id}:video} / {@code {id}:audio} ata cada archivo a su
 *       video y a su pista: no se pueden intercambiar entre videos ni entre pistas.</li>
 *   <li>La DEK se envuelve con HPKE usando la clave <b>pública</b> del Streaming Server, con AAD =
 *       {@code videoId}: el Core no puede volver a abrirla y una DEK copiada a otro video no sirve.</li>
 * </ol>
 *
 * Se envuelven los 32 bytes de la clave (no el keyset serializado) porque es lo que espera el
 * Streaming Server ({@code subtle.NewAESGCMHKDF} en {@code crypto.go}).
 */
@Service
@RequiredArgsConstructor
public class VideoEncryptionService implements IVideoEncryptionService {

    private final HpkePublicKey hpkePublicKey;

    @Override
    public EncryptedMedia encrypt(UUID videoId, UploadWorkspace workspace) throws ControllerException {
        byte[] dekBytes = null;
        try {
            KeysetHandle dek = KeysetHandle.generateNew(PredefinedStreamingAeadParameters.AES256_GCM_HKDF_1MB);
            StreamingAead streamingAead = dek.getPrimitive(RegistryConfiguration.get(), StreamingAead.class);

            EncryptedFile video = encryptFile(streamingAead, workspace.video(), workspace.encryptedVideo(),
                    aad(videoId + ":video"));
            EncryptedFile audio = encryptFile(streamingAead, workspace.audio(), workspace.encryptedAudio(),
                    aad(videoId + ":audio"));

            dekBytes = ((AesGcmHkdfStreamingKey) dek.getPrimary().getKey())
                    .getInitialKeyMaterial().toByteArray(InsecureSecretKeyAccess.get());
            byte[] wrappedDek = hpkePublicKey.hybridEncrypt().encrypt(dekBytes, aad(videoId.toString()));
            return new EncryptedMedia(video, audio, wrappedDek, hpkePublicKey.keyId());
        } catch (GeneralSecurityException | IOException ex) {
            throw new ControllerException(ErrorResponses.INTERNAL_SERVER_ERROR, ex);
        } finally {
            // La DEK en claro no debe quedarse en memoria más de lo necesario
            if (dekBytes != null) {
                Arrays.fill(dekBytes, (byte) 0);
            }
        }
    }

    /**
     * Cifra el archivo y, en la misma pasada, calcula el SHA-256 del resultado cifrado.
     */
    private static EncryptedFile encryptFile(StreamingAead streamingAead, Path source, Path target, byte[] aad)
            throws GeneralSecurityException, IOException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (OutputStream file = new DigestOutputStream(Files.newOutputStream(target), sha256);
             OutputStream encrypting = streamingAead.newEncryptingStream(file, aad)) {
            Files.copy(source, encrypting);
        }
        return new EncryptedFile(target, Files.size(target), HexFormat.of().formatHex(sha256.digest()));
    }

    private static byte[] aad(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
