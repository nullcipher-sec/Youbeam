package com.youbeam.core.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Carga la clave ECDSA P-256 con la que el Core firma los tickets de reproducción.
 *
 * Si la clave falta o no es P-256 la aplicación no arranca: es preferible fallar al inicio que
 * emitir tickets que el Streaming Server rechazará (o, peor, firmar con una clave débil).
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyConfig.class);

    private static final String PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_FOOTER = "-----END PRIVATE KEY-----";

    @Bean
    public ECKey ticketSigningKey(JwtProperties properties, Environment environment) throws GeneralSecurityException {
        if (properties.ephemeralKey()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("youbeam.jwt.ephemeral-key must not be enabled in prod");
            }
            logger.warn("Using an EPHEMERAL ticket signing key: tickets will not be accepted by the Streaming Server");
            return toJwk(generateKeyPair());
        }

        Path keyFile = properties.privateKeyLocations().stream()
                .map(Path::of)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "JWT signing key not found in " + properties.privateKeyLocations()
                                + ". Generate it as described in docs/GUIA-SEGURIDAD.md (step 1)"));

        ECPrivateKey privateKey = readPrivateKey(keyFile);
        ECKey key = toJwk(new KeyPair(derivePublicKey(privateKey), privateKey));
        // Se registra solo después de validar la curva, para no anunciar una clave que se va a rechazar
        logger.info("Ticket signing key loaded from {} (kid={})", keyFile, key.getKeyID());
        return key;
    }

    /**
     * Lee una clave PKCS#8 sin cifrar ({@code openssl pkcs8 -topk8 -nocrypt}).
     */
    private static ECPrivateKey readPrivateKey(Path keyFile) throws GeneralSecurityException {
        String pem;
        try {
            pem = Files.readString(keyFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read JWT signing key " + keyFile, ex);
        }
        if (!pem.contains(PEM_HEADER)) {
            throw new IllegalStateException("JWT signing key " + keyFile + " must be a PKCS#8 PEM (BEGIN PRIVATE KEY)");
        }
        String base64 = pem.substring(pem.indexOf(PEM_HEADER) + PEM_HEADER.length(), pem.indexOf(PEM_FOOTER))
                .replaceAll("\\s", "");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64));
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(spec);
    }

    /**
     * Calcula la clave pública Q = d·G a partir de la privada, para no depender de un segundo archivo.
     */
    private static ECPublicKey derivePublicKey(ECPrivateKey privateKey) throws GeneralSecurityException {
        ECNamedCurveParameterSpec curve = ECNamedCurveTable.getParameterSpec("secp256r1");
        org.bouncycastle.math.ec.ECPoint q = curve.getG().multiply(privateKey.getS()).normalize();
        ECPoint point = new ECPoint(q.getAffineXCoord().toBigInteger(), q.getAffineYCoord().toBigInteger());
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, privateKey.getParams()));
    }

    private static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static ECKey toJwk(KeyPair keyPair) {
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        // ES256 exige P-256; una clave de otra curva se rechaza
        if (!Curve.P_256.equals(Curve.forECParameterSpec(publicKey.getParams()))) {
            throw new IllegalStateException("JWT signing key must use curve P-256 (prime256v1)");
        }
        try {
            return new ECKey.Builder(Curve.P_256, publicKey)
                    .privateKey((ECPrivateKey) keyPair.getPrivate())
                    .keyIDFromThumbprint()
                    .build();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Cannot compute JWT key id", ex);
        }
    }
}
