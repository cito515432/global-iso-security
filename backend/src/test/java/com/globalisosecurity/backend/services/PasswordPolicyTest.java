package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(12);

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> policy.validate("GlobalISO-2026-Segura"));
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(BadRequestException.class, () -> policy.validate("Aa1!short"));
    }

    @Test
    void rejectsLowDiversityPassword() {
        assertThrows(BadRequestException.class, () -> policy.validate("solominusculaslargas"));
    }

    @Test
    void rejectsControlCharacters() {
        assertThrows(BadRequestException.class, () -> policy.validate("GlobalISO2026!\n"));
    }
}
