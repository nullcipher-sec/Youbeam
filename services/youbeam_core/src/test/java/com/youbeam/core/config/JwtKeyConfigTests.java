package com.youbeam.core.config;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.youbeam.core.support.TestKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtKeyConfigTests {

    private final JwtKeyConfig config = new JwtKeyConfig();
    private final MockEnvironment dev = new MockEnvironment();

    @TempDir
    Path dir;

    @Test
    void loadsPkcs8PemAndDerivesTheMatchingPublicKey() throws Exception {
        KeyPair keyPair = TestKeys.generate("secp256r1");
        Path pem = TestKeys.writePkcs8Pem(keyPair, dir.resolve("jwt_private.pem"));

        ECKey key = config.ticketSigningKey(properties(false, pem.toString()), dev);

        assertThat(key.getCurve()).isEqualTo(Curve.P_256);
        assertThat(key.isPrivate()).isTrue();
        assertThat(key.toECPublicKey()).isEqualTo((ECPublicKey) keyPair.getPublic());
        assertThat(key.getKeyID()).isNotBlank();
    }

    @Test
    void usesTheFirstLocationThatExists() throws Exception {
        Path pem = TestKeys.writePkcs8Pem(TestKeys.generate("secp256r1"), dir.resolve("jwt_private.pem"));

        ECKey key = config.ticketSigningKey(properties(false, dir.resolve("missing.pem").toString(), pem.toString()), dev);

        assertThat(key.isPrivate()).isTrue();
    }

    @Test
    void missingKeyPreventsStartup() {
        assertThatThrownBy(() -> config.ticketSigningKey(properties(false, dir.resolve("missing.pem").toString()), dev))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT signing key not found");
    }

    @Test
    void keyOnAnotherCurveIsRejected() throws Exception {
        Path pem = TestKeys.writePkcs8Pem(TestKeys.generate("secp384r1"), dir.resolve("p384.pem"));

        assertThatThrownBy(() -> config.ticketSigningKey(properties(false, pem.toString()), dev))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("P-256");
    }

    @Test
    void sec1PemIsRejectedWithAClearMessage() throws Exception {
        Path pem = Files.writeString(dir.resolve("sec1.pem"),
                "-----BEGIN EC PRIVATE KEY-----\nAAAA\n-----END EC PRIVATE KEY-----\n");

        assertThatThrownBy(() -> config.ticketSigningKey(properties(false, pem.toString()), dev))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PKCS#8");
    }

    @Test
    void ephemeralKeyIsForbiddenInProd() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThatThrownBy(() -> config.ticketSigningKey(properties(true), prod))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod");
    }

    private static JwtProperties properties(boolean ephemeral, String... locations) {
        return new JwtProperties(List.of(locations), ephemeral, "youbeam-core", "youbeam-stream", Duration.ofSeconds(60));
    }
}
