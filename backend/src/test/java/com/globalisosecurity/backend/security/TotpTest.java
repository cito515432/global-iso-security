package com.globalisosecurity.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class TotpTest {

    private static final String RFC_TEST_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void followsRfc6238HmacSha1SixDigitVector() {
        assertEquals("287082", Totp.codeFor(RFC_TEST_SECRET, 59_000L));
    }

    @Test
    void acceptsAdjacentClockStepAndRejectsReplay() {
        long time = 90_000L;
        String previousStepCode = Totp.codeFor(RFC_TEST_SECRET, 60_000L);
        OptionalLong accepted = Totp.matchingStep(RFC_TEST_SECRET, previousStepCode, time, null);
        assertTrue(accepted.isPresent());
        assertEquals(2L, accepted.getAsLong());
        assertFalse(Totp.matchingStep(RFC_TEST_SECRET, previousStepCode, time, accepted.getAsLong()).isPresent());
    }

    @Test
    void createsTwentyByteBase32Secrets() {
        String secret = Totp.generateSecret();
        assertEquals(32, secret.length());
        assertTrue(secret.matches("[A-Z2-7]+"));
    }
}
