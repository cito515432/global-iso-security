package com.globalisosecurity.backend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BcryptSha256PasswordEncoderTest {

    private final BcryptSha256PasswordEncoder encoder = new BcryptSha256PasswordEncoder();

    @Test
    void roundTripsLongUtf8PasswordsWithoutStoringRawValue() {
        String password = "Frase extensa con caracteres internacionales: seguridad y protección 2026";
        String hash = encoder.encode(password);
        assertTrue(hash.startsWith("{bcrypt-sha256}$2a$12$"));
        assertTrue(encoder.matches(password, hash));
        assertFalse(encoder.matches(password + "x", hash));
        assertFalse(hash.contains(password));
    }

    @Test
    void continuesToVerifyExistingBcryptHashes() {
        String password = "ClaveLegada-2026!";
        String oldHash = new BCryptPasswordEncoder(10).encode(password);
        assertTrue(encoder.matches(password, oldHash));
        assertFalse(encoder.matches(password + "x", oldHash));
    }

    @Test
    void rejectsAmbiguousLegacyPasswordsLongerThanBcryptLimit() {
        String prefix = "a".repeat(72);
        String oldHash = new BCryptPasswordEncoder(10).encode(prefix);
        assertFalse(encoder.matches(prefix + "different", oldHash));
    }
}
