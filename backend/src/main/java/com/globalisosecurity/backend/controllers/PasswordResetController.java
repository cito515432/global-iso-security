package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.services.PasswordResetService;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start(@RequestBody(required = false) StartRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.start(request == null ? null : request.email()));
    }

    @PostMapping("/complete")
    public ResponseEntity<Map<String, String>> complete(@RequestBody(required = false) CompleteRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.complete(
                request == null ? null : request.token(),
                request == null ? null : request.password(),
                request == null ? null : request.confirmation()));
    }

    public record StartRequest(String email) {}
    public record CompleteRequest(String token, String password, String confirmation) {}
}
