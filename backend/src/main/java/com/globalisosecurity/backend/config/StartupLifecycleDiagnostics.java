package com.globalisosecurity.backend.config;

import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Instrumentación opt-in para diagnosticar el tramo final del startup.
 * No expone configuración, credenciales ni datos de negocio.
 */
@Component
@ConditionalOnProperty(name = "startup.diagnostics.enabled", havingValue = "true")
public class StartupLifecycleDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(StartupLifecycleDiagnostics.class);

    private final ApplicationContext context;
    private final ApplicationAvailability availability;

    public StartupLifecycleDiagnostics(
            ApplicationContext context,
            ApplicationAvailability availability) {
        this.context = context;
        this.availability = availability;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        log.info("STARTUP_DIAG APPLICATION_STARTED");
        log.info("STARTUP_DIAG command-line-runners={}",
                runnerSummary(context.getBeansOfType(CommandLineRunner.class)));
        log.info("STARTUP_DIAG application-runners={}",
                runnerSummary(context.getBeansOfType(ApplicationRunner.class)));
        log.info("STARTUP_DIAG liveness={}", availability.getLivenessState());
        log.info("STARTUP_DIAG readiness={}", availability.getReadinessState());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("STARTUP_DIAG APPLICATION_READY");
        log.info("STARTUP_DIAG readiness={}", availability.getReadinessState());
    }

    @EventListener(ApplicationFailedEvent.class)
    public void onApplicationFailed(ApplicationFailedEvent event) {
        Throwable failure = event.getException();
        log.error("STARTUP_DIAG APPLICATION_FAILED exceptionType={}",
                failure == null ? "unknown" : failure.getClass().getName());
    }

    @EventListener(AvailabilityChangeEvent.class)
    public void onAvailabilityChange(AvailabilityChangeEvent<?> event) {
        Object state = event.getState();
        if (state instanceof ReadinessState) {
            log.info("STARTUP_DIAG READINESS={}", state);
        } else {
            log.info("STARTUP_DIAG AVAILABILITY stateType={}",
                    state == null ? "unknown" : state.getClass().getSimpleName());
        }
    }

    private String runnerSummary(Map<String, ?> beans) {
        return beans.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue().getClass().getName())
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));
    }
}
