package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterConfiguredMinimumAndClearsOnSuccess() {
        LoginAttemptService service = new LoginAttemptService(3, 15, 15, 10);

        assertTrue(service.recordFailure("user|ip"));
        assertTrue(service.recordFailure("user|ip"));
        assertTrue(service.recordFailure("user|ip"));
        assertTrue(service.isBlocked("user|ip"));

        service.recordSuccess("user|ip");
        assertFalse(service.isBlocked("user|ip"));
    }

    @Test
    void deniesNewTrackedKeysOnceBoundedStoreIsFull() {
        LoginAttemptService service = new LoginAttemptService(5, 15, 15, 2);

        assertTrue(service.recordFailure("first|ip"));
        assertTrue(service.recordFailure("second|ip"));
        assertFalse(service.recordFailure("third|ip"));
    }
}
