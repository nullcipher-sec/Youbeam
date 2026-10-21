package com.youbeam.core.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * Genera claves EC de prueba y las escribe como PEM PKCS#8, igual que {@code openssl pkcs8 -topk8 -nocrypt}.
 */
public final class TestKeys {

    private TestKeys() {
    }

    public static KeyPair generate(String curve) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    public static Path writePkcs8Pem(KeyPair keyPair, Path file) throws IOException {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded());
        Files.writeString(file, "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n");
        return file;
    }
}
