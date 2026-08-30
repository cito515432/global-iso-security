package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.config.ApplicationReadiness;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final ApplicationReadiness readiness;

    public HealthController(ApplicationReadiness readiness) {
        this.readiness = readiness;
    }

    @GetMapping({"/health", "/api/health"})
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping({"/readiness", "/api/readiness"})
    public ResponseEntity<Map<String, String>> readiness() {
        if (readiness.isReady()) {
            return ResponseEntity.ok(Map.of("status", "ready"));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "starting"));
    }
}
