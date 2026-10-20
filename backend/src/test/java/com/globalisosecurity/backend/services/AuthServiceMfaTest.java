package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.security.BcryptSha256PasswordEncoder;
import com.globalisosecurity.backend.utils.JwtUtil;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AuthServiceMfaTest {

    @Test
    void passwordLoginIssuesJwtWithoutMfaChallenge() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        JwtUtil jwt = mock(JwtUtil.class);
        MfaService mfa = mock(MfaService.class);
        LogAuditoriaService logs = mock(LogAuditoriaService.class);
        BcryptSha256PasswordEncoder encoder = new BcryptSha256PasswordEncoder();
        LoginAttemptService attempts = new LoginAttemptService(5, 15, 15, 100);

        Rol role = new Rol();
        role.setId(4L);
        role.setNombre("USUARIO_EMPRESA");
        role.setPermisos("{\"portalEmpresa\":true}");
        role.setActivo(true);

        Usuario user = new Usuario();
        user.setId(8L);
        user.setEmail("empresa@example.test");
        user.setNombre("Empresa Prueba");
        user.setPassword(encoder.encode("Luna azul sobre montañas, viento y café"));
        user.setRol(role);

        when(users.findByEmail("empresa@example.test")).thenReturn(Optional.of(user));
        when(mfa.sessionVersionForUserId(user.getId())).thenReturn(3L);
        when(mfa.isConfigured(user.getId())).thenReturn(true);
        when(jwt.generarToken(user.getEmail(), role.getNombre(), 3L)).thenReturn("signed-jwt");

        AuthService auth = new AuthService(users, jwt, encoder, attempts, mfa, logs);
        ResponseEntity<?> authenticated = auth.login(
                user.getEmail(), "Luna azul sobre montañas, viento y café");

        assertEquals(200, authenticated.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) authenticated.getBody();
        assertEquals("signed-jwt", body.get("token"));
        assertTrue(Boolean.TRUE.equals(body.get("mfaConfigured")));
        verify(mfa, never()).start(any(Usuario.class));
    }

    @Test
    void accountRecoveryStartsAnExplicitMfaChallenge() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        JwtUtil jwt = mock(JwtUtil.class);
        MfaService mfa = mock(MfaService.class);
        LogAuditoriaService logs = mock(LogAuditoriaService.class);
        BcryptSha256PasswordEncoder encoder = new BcryptSha256PasswordEncoder();
        LoginAttemptService attempts = new LoginAttemptService(5, 15, 15, 100);

        Rol role = new Rol();
        role.setId(4L);
        role.setNombre("USUARIO_EMPRESA");
        role.setActivo(true);

        Usuario user = new Usuario();
        user.setId(8L);
        user.setEmail("empresa@example.test");
        user.setRol(role);

        when(users.findByEmail("empresa@example.test")).thenReturn(Optional.of(user));
        when(mfa.startRecovery(user)).thenReturn(new MfaService.StartResult(false,
                Map.of("mfaRequired", true, "recoveryAvailable", true, "challengeToken", "opaque-challenge")));

        AuthService auth = new AuthService(users, jwt, encoder, attempts, mfa, logs);
        ResponseEntity<?> response = auth.startRecovery("empresa@example.test");

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("mfaRequired"));
        assertEquals("opaque-challenge", body.get("challengeToken"));
        verify(mfa).startRecovery(user);
    }

    @Test
    void recoveryVerificationIssuesJwtAfterMfa() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        JwtUtil jwt = mock(JwtUtil.class);
        MfaService mfa = mock(MfaService.class);
        LogAuditoriaService logs = mock(LogAuditoriaService.class);
        BcryptSha256PasswordEncoder encoder = new BcryptSha256PasswordEncoder();
        LoginAttemptService attempts = new LoginAttemptService(5, 15, 15, 100);

        Rol role = new Rol();
        role.setId(4L);
        role.setNombre("USUARIO_EMPRESA");
        role.setPermisos("{\"portalEmpresa\":true}");
        role.setActivo(true);

        Usuario user = new Usuario();
        user.setId(8L);
        user.setEmail("empresa@example.test");
        user.setNombre("Empresa Prueba");
        user.setRol(role);

        when(mfa.verifyRecovery("opaque-challenge", "123456"))
                .thenReturn(new MfaService.VerificationResult(true, user.getId(), false, List.of()));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(mfa.sessionVersionForUserId(user.getId())).thenReturn(3L);
        when(mfa.isConfigured(user.getId())).thenReturn(true);
        when(jwt.generarToken(user.getEmail(), role.getNombre(), 3L)).thenReturn("signed-jwt");

        AuthService auth = new AuthService(users, jwt, encoder, attempts, mfa, logs);
        ResponseEntity<?> response = auth.verifyRecovery("opaque-challenge", "123456");

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("signed-jwt", body.get("token"));
        verify(mfa).verifyRecovery("opaque-challenge", "123456");
    }
}
