package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.utils.JwtUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final MfaService mfaService;
    private final LogAuditoriaService logAuditoriaService;
    private final String dummyPasswordHash;

    public AuthService(UsuarioRepository usuarioRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttempts,
            MfaService mfaService,
            LogAuditoriaService logAuditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.mfaService = mfaService;
        this.logAuditoriaService = logAuditoriaService;
        // Reduce diferencias temporales entre correo inexistente y clave incorrecta.
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public ResponseEntity<?> login(String email, String password) {
        String rawEmail = email == null ? "" : email.trim();
        String normalizedEmail = rawEmail.length() > 254 ? "" : rawEmail.toLowerCase(Locale.ROOT);
        String rawPassword = password == null ? "" : password;
        String safePassword = rawPassword.length() > 128 ? "input-too-long" : rawPassword;
        String attemptKey = loginAttempts.keyFor(normalizedEmail);
        String accountAttemptKey = normalizedEmail.isBlank()
                ? attemptKey : loginAttempts.accountKeyFor(normalizedEmail);

        if (loginAttempts.isBlocked(attemptKey) || loginAttempts.isBlocked(accountAttemptKey)) {
            return response(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Intente nuevamente más tarde");
        }

        if (normalizedEmail.isBlank() || safePassword.isEmpty() || rawPassword.length() > 128) {
            boolean tracked = recordLoginFailure(attemptKey, accountAttemptKey);
            passwordEncoder.matches(safePassword, dummyPasswordHash);
            auditLogin(normalizedEmail, false);
            if (!tracked) return response(HttpStatus.TOO_MANY_REQUESTS, "Autenticación temporalmente limitada");
            return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(normalizedEmail);
        if (usuarioOpt.isEmpty()) {
            passwordEncoder.matches(safePassword, dummyPasswordHash);
            boolean tracked = recordLoginFailure(attemptKey, accountAttemptKey);
            auditLogin(normalizedEmail, false);
            if (!tracked) {
                return response(HttpStatus.TOO_MANY_REQUESTS, "Autenticación temporalmente limitada");
            }
            return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        Usuario usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(safePassword, usuario.getPassword())) {
            boolean tracked = recordLoginFailure(attemptKey, accountAttemptKey);
            auditLogin(normalizedEmail, false);
            if (!tracked) {
                return response(HttpStatus.TOO_MANY_REQUESTS, "Autenticación temporalmente limitada");
            }
            return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        if (RolPerfilUtil.activeRoles(usuario).isEmpty()) {
            boolean tracked = recordLoginFailure(attemptKey, accountAttemptKey);
            auditLogin(normalizedEmail, false);
            if (!tracked) {
                return response(HttpStatus.TOO_MANY_REQUESTS, "Autenticación temporalmente limitada");
            }
            return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        loginAttempts.recordSuccess(attemptKey);
        if (!accountAttemptKey.equals(attemptKey)) loginAttempts.recordSuccess(accountAttemptKey);
        auditLogin(normalizedEmail, true);
        // El acceso cotidiano usa el primer factor (correo + contraseña).
        // MFA se reserva para el flujo explícito de recuperación de cuenta.
        return authenticatedResponse(usuario, List.of());
    }

    public ResponseEntity<?> startRecovery(String email) {
        String rawEmail = email == null ? "" : email.trim();
        String normalizedEmail = rawEmail.length() > 254 ? "" : rawEmail.toLowerCase(Locale.ROOT);
        String attemptKey = loginAttempts.keyFor("recovery:" + normalizedEmail);

        if (loginAttempts.isBlocked(attemptKey)) {
            return response(HttpStatus.TOO_MANY_REQUESTS,
                    "La recuperación está temporalmente limitada. Intente nuevamente más tarde");
        }

        if (!loginAttempts.recordFailure(attemptKey)) {
            return response(HttpStatus.TOO_MANY_REQUESTS,
                    "La recuperación está temporalmente limitada. Intente nuevamente más tarde");
        }

        Optional<Usuario> usuarioOpt = normalizedEmail.isBlank()
                ? Optional.empty()
                : usuarioRepository.findByEmail(normalizedEmail);
        if (usuarioOpt.isEmpty()) {
            // Respuesta uniforme: no revela si el correo está registrado.
            passwordEncoder.matches("", dummyPasswordHash);
            return recoveryUnavailableResponse();
        }

        Usuario usuario = usuarioOpt.get();
        if (RolPerfilUtil.activeRoles(usuario).isEmpty()) {
            return recoveryUnavailableResponse();
        }

        MfaService.StartResult challenge = mfaService.startRecovery(usuario);
        if (challenge.rateLimited()) {
            return response(HttpStatus.TOO_MANY_REQUESTS,
                    "La recuperación está temporalmente bloqueada. Intente nuevamente más tarde");
        }
        if (!Boolean.TRUE.equals(challenge.body().get("mfaRequired"))) {
            return recoveryUnavailableResponse();
        }
        return response(HttpStatus.OK, challenge.body());
    }

    public ResponseEntity<?> verifyRecovery(String challengeToken, String code) {
        return verifyMfa(challengeToken, code, true);
    }

    public ResponseEntity<?> verifyMfa(String challengeToken, String code) {
        return verifyMfa(challengeToken, code, false);
    }

    private ResponseEntity<?> verifyMfa(String challengeToken, String code, boolean recoveryOnly) {
        MfaService.VerificationResult result = recoveryOnly
                ? mfaService.verifyRecovery(challengeToken, code)
                : mfaService.verify(challengeToken, code);
        if (!result.success()) {
            HttpStatus status = result.rateLimited() ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNAUTHORIZED;
            return response(status, "El código MFA no es válido o el reto expiró");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(result.userId());
        if (usuarioOpt.isEmpty()) {
            return response(HttpStatus.UNAUTHORIZED, "No fue posible completar la autenticación");
        }
        Usuario usuario = usuarioOpt.get();
        if (RolPerfilUtil.activeRoles(usuario).isEmpty()) {
            return response(HttpStatus.FORBIDDEN, "La cuenta o el rol no están activos");
        }
        return authenticatedResponse(usuario, result.recoveryCodes());
    }

    private ResponseEntity<?> recoveryUnavailableResponse() {
        return response(HttpStatus.OK, Map.of(
                "mfaRequired", false,
                "recoveryAvailable", false,
                "message", "Si la cuenta existe y tiene MFA configurado, podrá continuar con la recuperación"));
    }

    @Transactional
    public ResponseEntity<?> resetMfaByAdministrator(Long userId) {
        Optional<Usuario> usuario = usuarioRepository.findById(userId);
        if (usuario.isEmpty()) {
            return response(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        mfaService.reset(userId);
        logAuditoriaService.registrarLog("MFA_RESET", "SEGURIDAD",
                "Se restableció el segundo factor del usuario id=" + userId);
        return response(HttpStatus.OK,
                Map.of("message", "MFA restablecido; la cuenta podrá iniciar sesión con contraseña y el administrador podrá coordinar una nueva activación"));
    }

    private ResponseEntity<?> authenticatedResponse(Usuario usuario, List<String> recoveryCodes) {
        List<com.globalisosecurity.backend.models.Rol> activeRoles = RolPerfilUtil.activeRoles(usuario);
        if (activeRoles.isEmpty()) return response(HttpStatus.FORBIDDEN, "La cuenta no tiene roles activos");
        var primaryRole = activeRoles.stream().filter(role -> usuario.getRol() != null
                && role.getId().equals(usuario.getRol().getId())).findFirst().orElse(activeRoles.get(0));
        long sessionVersion = mfaService.sessionVersionForUserId(usuario.getId());
        String sessionId = mfaService.createSession(usuario.getId());
        String token = jwtUtil.generarToken(usuario.getEmail(), primaryRole.getNombre(), sessionVersion, sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", usuario.getEmail());
        response.put("nombre", usuario.getNombre());
        response.put("rol", primaryRole.getNombre());
        response.put("perfilBase", RolPerfilUtil.effectiveProfile(primaryRole));
        response.put("rolId", primaryRole.getId());
        response.put("rolActivo", primaryRole.getActivo());
        response.put("roles", activeRoles.stream().map(role -> Map.of(
                "id", role.getId(), "nombre", role.getNombre(),
                "perfilBase", RolPerfilUtil.effectiveProfile(role))).toList());
        LinkedHashSet<String> effectivePermissions = new LinkedHashSet<>();
        for (var role : activeRoles) effectivePermissions.addAll(RolPerfilUtil.permissionAuthorities(role));
        response.put("permisos", primaryRole.getPermisos());
        response.put("permisosEfectivos", List.copyOf(effectivePermissions));
        response.put("mfaConfigured", mfaService.isConfigured(usuario.getId()));
        if (!recoveryCodes.isEmpty()) response.put("recoveryCodes", recoveryCodes);
        if (usuario.getEmpresa() != null) {
            response.put("empresaId", usuario.getEmpresa().getId());
            response.put("empresaNombre", usuario.getEmpresa().getNombre());
        } else {
            response.put("empresaId", null);
            response.put("empresaNombre", null);
        }
        return response(HttpStatus.OK, response);
    }

    @Transactional
    public ResponseEntity<?> logout(String email) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(
                email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        if (usuario.isPresent()) {
            mfaService.revokeSessions(usuario.get().getId());
            logAuditoriaService.registrarEvento(usuario.get().getEmail(), "LOGOUT", "AUTENTICACION",
                    "Se revocaron las sesiones activas de la cuenta");
        }
        return response(HttpStatus.OK, Map.of("message", "Sesiones cerradas"));
    }

    private ResponseEntity<?> response(HttpStatus status, Object body) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    private void auditLogin(String email, boolean success) {
        try {
            String actor = email == null || email.length() > 254 ? "IDENTIFICADOR_INVALIDO" : email;
            logAuditoriaService.registrarEvento(actor, success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE",
                    "AUTENTICACION", success ? "Inicio de sesión completado" : "Intento de inicio de sesión fallido");
        } catch (Exception ignored) {
            // Audit storage must not change the authentication result.
        }
    }

    private boolean recordLoginFailure(String clientKey, String accountKey) {
        boolean clientTracked = loginAttempts.recordFailure(clientKey);
        boolean accountTracked = accountKey.equals(clientKey) || loginAttempts.recordFailure(accountKey);
        if (!clientTracked || !accountTracked) return false;
        // Return the blocked state immediately on the fifth failure, not one request later.
        return !loginAttempts.isBlocked(clientKey) && !loginAttempts.isBlocked(accountKey);
    }
}
