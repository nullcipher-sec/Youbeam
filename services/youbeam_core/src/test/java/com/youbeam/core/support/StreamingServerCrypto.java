package com.youbeam.core.support;

import com.google.crypto.tink.subtle.AesGcmHkdfStreaming;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

/**
 * Descifra exactamente como el Streaming Server ({@code crypto.go}): HPKE con AAD = videoId para la
 * DEK, y {@code subtle.NewAESGCMHKDF(dek, "SHA256", 32, 1<<20, 0)} para los archivos.
 */
public final class StreamingServerCrypto {

    private StreamingServerCrypto() {
    }

    public static byte[] unwrap(byte[] wrappedDek, String videoId) throws GeneralSecurityException {
        return TestHpkeKeys.hybridDecrypt().decrypt(wrappedDek, videoId.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] decrypt(byte[] dek, Path file, String aad) throws Exception {
        return decrypt(dek, Files.readAllBytes(file), aad);
    }

    public static byte[] decrypt(byte[] dek, byte[] ciphertext, String aad) throws Exception {
        AesGcmHkdfStreaming streaming = new AesGcmHkdfStreaming(dek, "HmacSha256", 32, 1 << 20, 0);
        try (InputStream in = streaming.newDecryptingStream(new ByteArrayInputStream(ciphertext),
                aad.getBytes(StandardCharsets.UTF_8))) {
            return in.readAllBytes();
        }
    }
}
