package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
    void passwordStepNeverIssuesJwtAndMfaVerificationDoes() {
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
        when(mfa.start(user)).thenReturn(new MfaService.StartResult(false,
                Map.of("mfaRequired", true, "challengeToken", "opaque-challenge")));
        when(mfa.verify("opaque-challenge", "123456"))
                .thenReturn(new MfaService.VerificationResult(true, user.getId(), false, List.of()));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(mfa.sessionVersionForUserId(user.getId())).thenReturn(3L);
        when(jwt.generarToken(user.getEmail(), role.getNombre(), 3L)).thenReturn("signed-jwt");

        AuthService auth = new AuthService(users, jwt, encoder, attempts, mfa, logs);
        ResponseEntity<?> firstFactor = auth.login(user.getEmail(), "Luna azul sobre montañas, viento y café");

        assertEquals(200, firstFactor.getStatusCode().value());
        Map<?, ?> challenge = (Map<?, ?>) firstFactor.getBody();
        assertEquals(true, challenge.get("mfaRequired"));
        assertFalse(challenge.containsKey("token"));

        ResponseEntity<?> secondFactor = auth.verifyMfa("opaque-challenge", "123456");
        Map<?, ?> authenticated = (Map<?, ?>) secondFactor.getBody();
        assertEquals("signed-jwt", authenticated.get("token"));
        assertTrue(Boolean.TRUE.equals(authenticated.get("mfaConfigured")));
    }
}
