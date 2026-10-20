package com.globalisosecurity.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Stores new passwords as BCrypt(SHA-256(password)) to avoid BCrypt's 72-byte
 * input truncation. Unprefixed historical BCrypt hashes remain verifiable.
 */
public final class BcryptSha256PasswordEncoder implements PasswordEncoder {

    private static final String PREFIX = "{bcrypt-sha256}";
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) throw new IllegalArgumentException("La contraseña es obligatoria");
        return PREFIX + bcrypt.encode(prehash(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        if (encodedPassword.startsWith(PREFIX)) {
            return bcrypt.matches(prehash(rawPassword), encodedPassword.substring(PREFIX.length()));
        }
        // Old BCrypt hashes truncate at 72 UTF-8 bytes. Do not preserve that ambiguity.
        if (rawPassword.toString().getBytes(StandardCharsets.UTF_8).length > 72) return false;
        try {
            return bcrypt.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String prehash(CharSequence rawPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible procesar la contraseña", ex);
        }
    }
}
