package com.youbeam.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generación y hash de tokens de sesión.
 *
 * El token tiene 256 bits aleatorios, así que no se puede adivinar por fuerza bruta y basta
 * con SHA-256 (rápido) para guardarlo: a diferencia de una contraseña, no hace falta un hash
 * lento como Argon2. Al ser determinista, el hash también sirve para buscar la sesión.
 */
public final class SessionTokens {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SessionTokens() {
    }

    /**
     * Genera un token de 256 bits codificado en Base64 URL-safe (43 caracteres).
     */
    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    /**
     * Devuelve {@code SHA-256(token)} en hexadecimal (64 caracteres), que es lo único que se guarda.
     */
    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // Toda JVM está obligada a implementar SHA-256
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
