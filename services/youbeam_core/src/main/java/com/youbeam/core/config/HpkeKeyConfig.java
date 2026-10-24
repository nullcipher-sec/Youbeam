package com.youbeam.core.config;

import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.hybrid.HpkeParameters;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

/**
 * Carga la clave pública HPKE del Streaming Server.
 *
 * El Core solo puede cifrar, nunca descifrar: {@code parseKeysetWithoutSecret} rechaza cualquier
 * keyset que contenga material secreto, así que si por error se monta la clave privada aquí la
 * aplicación no arranca. Tampoco arranca si la clave no es HPKE X25519 con AES-256-GCM.
 */
@Configuration
@EnableConfigurationProperties(HpkeProperties.class)
public class HpkeKeyConfig {

    private static final Logger logger = LoggerFactory.getLogger(HpkeKeyConfig.class);

    /**
     * Keyset público e id de su clave primaria (se guarda en {@code video_keys.key_id} para poder
     * rotar la clave más adelante).
     */
    public record HpkePublicKey(HybridEncrypt hybridEncrypt, String keyId) {
    }

    @Bean
    public HpkePublicKey hpkePublicKey(HpkeProperties properties) throws GeneralSecurityException {
        HybridConfig.register();
        StreamingAeadConfig.register();

        Path keyFile = properties.publicKeyLocations().stream()
                .map(Path::of)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "HPKE public key not found in " + properties.publicKeyLocations()
                                + ". Generate it as described in docs/GUIA-SEGURIDAD.md (step 1)"));

        KeysetHandle keyset;
        try {
            keyset = TinkJsonProtoKeysetFormat.parseKeysetWithoutSecret(Files.readString(keyFile));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read HPKE public key " + keyFile, ex);
        }
        validate(keyset, keyFile);

        String keyId = Integer.toUnsignedString(keyset.getPrimary().getId());
        logger.info("HPKE public key loaded from {} (keyId={})", keyFile, keyId);
        return new HpkePublicKey(keyset.getPrimitive(RegistryConfiguration.get(), HybridEncrypt.class), keyId);
    }

    private static void validate(KeysetHandle keyset, Path keyFile) {
        if (!(keyset.getPrimary().getKey().getParameters() instanceof HpkeParameters params)
                || params.getKemId() != HpkeParameters.KemId.DHKEM_X25519_HKDF_SHA256
                || params.getKdfId() != HpkeParameters.KdfId.HKDF_SHA256
                || params.getAeadId() != HpkeParameters.AeadId.AES_256_GCM) {
            throw new IllegalStateException("HPKE key " + keyFile
                    + " must be DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM");
        }
    }
}
