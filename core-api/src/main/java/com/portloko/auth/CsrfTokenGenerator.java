package com.portloko.auth;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitaire pour générer et valider les tokens CSRF
 */
public class CsrfTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits

    /**
     * Génère un token CSRF aléatoire et sécurisé
     * @return token CSRF en base64
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Valide que le token CSRF n'est pas null et n'est pas vide
     * @param token le token à valider
     * @return true si le token est valide
     */
    public static boolean isValidToken(String token) {
        return token != null && !token.isBlank() && token.length() >= 32;
    }
}
