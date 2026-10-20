package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(12);

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> policy.validate("Luna azul sobre montañas, viento y café"));
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(BadRequestException.class, () -> policy.validate("ab"));
    }

    @Test
    void rejectsCommonPasswordFragment() {
        assertThrows(BadRequestException.class, () -> policy.validate("passwordpassword"));
    }

    @Test
    void rejectsRepeatedShortPattern() {
        assertThrows(BadRequestException.class, () -> policy.validate("abcabcabcabcabc"));
    }

    @Test
    void rejectsControlCharacters() {
        assertThrows(BadRequestException.class, () -> policy.validate("GlobalISO2026!\n"));
    }
}
