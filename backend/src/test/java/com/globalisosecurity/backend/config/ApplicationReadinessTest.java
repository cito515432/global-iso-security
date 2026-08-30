package com.globalisosecurity.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationReadinessTest {

    @Test
    void startsNotReady() {
        assertThat(new ApplicationReadiness().isReady()).isFalse();
    }

    @Test
    void becomesReadyAfterApplicationReadyEvent() {
        ApplicationReadiness readiness = new ApplicationReadiness();

        readiness.markReady();

        assertThat(readiness.isReady()).isTrue();
    }
}
