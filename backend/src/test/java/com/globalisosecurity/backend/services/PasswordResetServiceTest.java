package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.security.BcryptSha256PasswordEncoder;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.mockito.ArgumentCaptor;

class PasswordResetServiceTest {

    @Test
    void resetTokenIsStoredHashedAndCanOnlyBeUsedOnce() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:password_reset_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE login_attempt_buckets (bucket_key CHAR(64) PRIMARY KEY, failures INT NOT NULL, "
                + "window_started_at TIMESTAMP NOT NULL, blocked_until TIMESTAMP NULL, updated_at TIMESTAMP NOT NULL)");
        jdbc.execute("CREATE TABLE password_reset_tokens (id BIGINT AUTO_INCREMENT PRIMARY KEY, usuario_id BIGINT NOT NULL, "
                + "token_hash CHAR(64) NOT NULL UNIQUE, expires_at TIMESTAMP NOT NULL, used_at TIMESTAMP NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        UsuarioRepository users = mock(UsuarioRepository.class);
        PasswordResetMailer mailer = mock(PasswordResetMailer.class);
        MfaService mfa = mock(MfaService.class);
        LogAuditoriaService audit = mock(LogAuditoriaService.class);
        BcryptSha256PasswordEncoder encoder = new BcryptSha256PasswordEncoder();
        LoginAttemptService attempts = new LoginAttemptService(jdbc, 5, 15, 15);
        PasswordResetService service = new PasswordResetService(jdbc, users, attempts, encoder,
                new PasswordPolicy(8), mfa, mailer, audit, true,
                "https://globaliso.example.test/pages/login.html", "security@example.test");

        Rol role = new Rol();
        role.setNombre("USUARIO_EMPRESA");
        role.setActivo(true);
        Usuario user = new Usuario();
        user.setId(81L);
        user.setEmail("empresa@example.test");
        user.setRol(role);
        user.setPassword(encoder.encode("ClaveAnterior2026!"));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(users.save(any(Usuario.class))).thenReturn(user);
        when(mailer.isConfigured()).thenReturn(true);
        String loginBucketKey = attempts.keyFor(user.getEmail());
        attempts.recordFailure(loginBucketKey);

        service.start(user.getEmail());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendResetLink(eq(user.getEmail()), eq("security@example.test"), link.capture());
        String token = URI.create(link.getValue()).getFragment().substring("resetToken=".length());
        String storedHash = jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE usuario_id = 81", String.class);
        assertEquals(64, storedHash.length());
        assertFalse(storedHash.equals(token));

        service.complete(token, "ClaveNueva2026!", "ClaveNueva2026!");
        assertFalse(encoder.matches("ClaveAnterior2026!", user.getPassword()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE used_at IS NOT NULL", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM login_attempt_buckets WHERE bucket_key = ?", Integer.class, loginBucketKey));
        verify(mfa).revokeSessions(user.getId());
        verify(audit).registrarEvento(eq(user.getEmail()), eq("PASSWORD_RESET"), eq("AUTENTICACION"), any());

        assertThrows(BadRequestException.class, () -> service.complete(token, "ClaveNueva2026!", "ClaveNueva2026!"));
    }
}
