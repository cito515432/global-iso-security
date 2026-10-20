package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return authService.login(request == null ? null : request.getEmail(),
                request == null ? null : request.getPassword());
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest request) {
        return authService.verifyMfa(request == null ? null : request.getChallengeToken(),
                request == null ? null : request.getCode());
    }

    @PostMapping("/mfa/admin-reset/{userId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> resetMfa(@PathVariable Long userId) {
        return authService.resetMfaByAdministrator(userId);
    }

    static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class MfaVerifyRequest {
        private String challengeToken;
        private String code;

        public String getChallengeToken() { return challengeToken; }
        public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
