package com.globalisosecurity.backend.config;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Readiness del ciclo de Spring. No realiza consultas ni trabajo de red.
 */
@Component
public class ApplicationReadiness {

    private static final Logger log = LoggerFactory.getLogger(ApplicationReadiness.class);

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void markReady() {
        log.info("STARTUP_DIAG ApplicationReadiness.markReady invoked");
        ready.set(true);
    }

    public boolean isReady() {
        return ready.get();
    }
}
