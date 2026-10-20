package com.globalisosecurity.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MfaSecretCryptoTest {

    private static final String TEST_KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";

    @Test
    void encryptsWithRandomNonceAndRoundTrips() {
        MfaSecretCrypto crypto = new MfaSecretCrypto(TEST_KEY);
        String first = crypto.encrypt("TOTP-SEED-EXAMPLE");
        String second = crypto.encrypt("TOTP-SEED-EXAMPLE");
        assertNotEquals(first, second);
        assertEquals("TOTP-SEED-EXAMPLE", crypto.decrypt(first));
    }

    @Test
    void rejectsTamperedCiphertext() {
        MfaSecretCrypto crypto = new MfaSecretCrypto(TEST_KEY);
        String ciphertext = crypto.encrypt("TOTP-SEED-EXAMPLE");
        char last = ciphertext.charAt(ciphertext.length() - 1);
        String tampered = ciphertext.substring(0, ciphertext.length() - 1) + (last == 'A' ? 'B' : 'A');
        assertThrows(IllegalStateException.class, () -> crypto.decrypt(tampered));
    }

    @Test
    void failsClosedWhenEncryptionKeyIsMissingOrMalformed() {
        assertThrows(IllegalStateException.class, () -> new MfaSecretCrypto(""));
        assertThrows(IllegalStateException.class, () -> new MfaSecretCrypto("AQ=="));
    }
}
