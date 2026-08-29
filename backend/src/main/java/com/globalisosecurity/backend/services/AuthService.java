package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.utils.JwtUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final String dummyPasswordHash;

    public AuthService(UsuarioRepository usuarioRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttempts) {
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        // Se calcula una sola vez para reducir diferencias temporales entre un correo
        // inexistente y una contraseña incorrecta.
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public ResponseEntity<?> login(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String safePassword = password == null ? "" : password;
        String attemptKey = loginAttempts.keyFor(normalizedEmail);

        if (loginAttempts.isBlocked(attemptKey)) {
            return response(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Intente nuevamente más tarde");
        }

        if (normalizedEmail.isBlank() || normalizedEmail.length() > 254
                || safePassword.isEmpty() || safePassword.length() > 128) {
            loginAttempts.recordFailure(attemptKey);
            passwordEncoder.matches(safePassword, dummyPasswordHash);
            return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(normalizedEmail);
        if (usuarioOpt.isEmpty()) {
            passwordEncoder.matches(safePassword, dummyPasswordHash);
            loginAttempts.recordFailure(attemptKey);
            return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        Usuario usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(safePassword, usuario.getPassword())) {
            loginAttempts.recordFailure(attemptKey);
            return response(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        if (usuario.getRol() == null) {
            return response(HttpStatus.FORBIDDEN, "El usuario no tiene un rol asignado");
        }
        if (Boolean.FALSE.equals(usuario.getRol().getActivo())) {
            return response(HttpStatus.FORBIDDEN, "El rol del usuario está inactivo");
        }

        loginAttempts.recordSuccess(attemptKey);
        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().getNombre());

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("token", token);
        respuesta.put("email", usuario.getEmail());
        respuesta.put("nombre", usuario.getNombre());
        respuesta.put("rol", usuario.getRol().getNombre());
        respuesta.put("rolId", usuario.getRol().getId());
        respuesta.put("rolActivo", usuario.getRol().getActivo());
        respuesta.put("permisos", usuario.getRol().getPermisos());
        if (usuario.getEmpresa() != null) {
            respuesta.put("empresaId", usuario.getEmpresa().getId());
            respuesta.put("empresaNombre", usuario.getEmpresa().getNombre());
        } else {
            respuesta.put("empresaId", null);
            respuesta.put("empresaNombre", null);
        }

        return response(HttpStatus.OK, respuesta);
    }

    private ResponseEntity<?> response(HttpStatus status, Object body) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
