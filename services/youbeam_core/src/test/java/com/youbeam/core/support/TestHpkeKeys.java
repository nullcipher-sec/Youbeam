package com.youbeam.core.support;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.crypto.tink.hybrid.HpkeParameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

/**
 * Par de claves HPKE de prueba, generado una vez por ejecución de tests. La parte pública se escribe
 * en un archivo temporal para que el Core la cargue igual que en producción; la privada solo la usan
 * los tests para comprobar lo que haría el Streaming Server.
 */
public final class TestHpkeKeys {

    private static final KeysetHandle PRIVATE_KEYSET;
    private static final Path PUBLIC_KEY_FILE;

    static {
        try {
            HybridConfig.register();
            // Equivalente a la plantilla de Tinkey DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM
            PRIVATE_KEYSET = KeysetHandle.generateNew(HpkeParameters.builder()
                    .setVariant(HpkeParameters.Variant.TINK)
                    .setKemId(HpkeParameters.KemId.DHKEM_X25519_HKDF_SHA256)
                    .setKdfId(HpkeParameters.KdfId.HKDF_SHA256)
                    .setAeadId(HpkeParameters.AeadId.AES_256_GCM)
                    .build());
            PUBLIC_KEY_FILE = Files.createTempFile("hpke_public", ".json");
            PUBLIC_KEY_FILE.toFile().deleteOnExit();
            Files.writeString(PUBLIC_KEY_FILE,
                    TinkJsonProtoKeysetFormat.serializeKeysetWithoutSecret(PRIVATE_KEYSET.getPublicKeysetHandle()));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private TestHpkeKeys() {
    }

    public static Path publicKeyFile() {
        return PUBLIC_KEY_FILE;
    }

    public static KeysetHandle privateKeyset() {
        return PRIVATE_KEYSET;
    }

    public static HybridDecrypt hybridDecrypt() throws GeneralSecurityException {
        return PRIVATE_KEYSET.getPrimitive(RegistryConfiguration.get(), HybridDecrypt.class);
    }
}
