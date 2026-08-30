package com.globalisosecurity.backend;

import java.time.Instant;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class BackendApplication {

	private static final Logger STARTUP_LOG = LoggerFactory.getLogger(BackendApplication.class);

	public static void main(String[] args) {
		if (!diagnosticsEnabled()) {
			SpringApplication.run(BackendApplication.class, args);
			return;
		}

		long startNanos = System.nanoTime();
		SpringApplication application = new SpringApplication(BackendApplication.class);
		application.setApplicationStartup(new BufferingApplicationStartup(2048));
		application.addListeners(new StartupDiagnosticListener(startNanos));
		STARTUP_LOG.info("[STARTUP-DIAG] application-start timestamp={}", Instant.now());
		application.run(args);
	}

	private static boolean diagnosticsEnabled() {
		return "true".equalsIgnoreCase(System.getenv("STARTUP_DIAGNOSTICS"));
	}

	private static final class StartupDiagnosticListener implements ApplicationListener<ApplicationEvent> {

		private final long startNanos;

		private StartupDiagnosticListener(long startNanos) {
			this.startNanos = startNanos;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
			if (event instanceof ApplicationStartedEvent) {
				STARTUP_LOG.info("[STARTUP-DIAG] application-started elapsedMs={}", elapsedMs);
			} else if (event instanceof ApplicationReadyEvent) {
				STARTUP_LOG.info("[STARTUP-DIAG] application-ready elapsedMs={}", elapsedMs);
				writeReadyMarker();
			} else if (event instanceof ApplicationFailedEvent failedEvent) {
				Throwable failure = failedEvent.getException();
				String type = failure == null ? "unknown" : failure.getClass().getName();
				STARTUP_LOG.error("[STARTUP-DIAG] application-failed elapsedMs={} exceptionType={}", elapsedMs, type);
			}
		}

		private void writeReadyMarker() {
			String readyFile = System.getenv().getOrDefault(
					"STARTUP_DIAGNOSTIC_READY_FILE", "/tmp/backend-startup-ready");

			try {
				Files.writeString(Path.of(readyFile), Instant.now().toString() + System.lineSeparator(),
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				STARTUP_LOG.info("[STARTUP-DIAG] readiness-marker-written");
			} catch (IOException | RuntimeException exception) {
				STARTUP_LOG.warn("[STARTUP-DIAG] readiness-marker-failed exceptionType={}",
						exception.getClass().getName());
			}
		}
	}

}
