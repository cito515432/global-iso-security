package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;

/** Email-token password reset; tokens are random, stored as hashes, single-use and short-lived. */
@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TOKEN_TTL_SECONDS = 15 * 60;
    private static final String GENERIC_MESSAGE =
            "Si la cuenta existe, está activa y el correo está configurado, recibirá instrucciones para restablecer la contraseña.";

    private final JdbcTemplate jdbc;
    private final UsuarioRepository users;
    private final LoginAttemptService attempts;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwordPolicy;
    private final MfaService mfaService;
    private final PasswordResetMailer mailer;
    private final LogAuditoriaService audit;
    private final boolean enabled;
    private final String frontendUrl;
    private final String sender;

    public PasswordResetService(JdbcTemplate jdbc, UsuarioRepository users,
            LoginAttemptService attempts, PasswordEncoder encoder, PasswordPolicy passwordPolicy,
            MfaService mfaService, PasswordResetMailer mailer, LogAuditoriaService audit,
            @Value("${app.security.password-reset.enabled:false}") boolean enabled,
            @Value("${app.security.password-reset.frontend-url:}") String frontendUrl,
            @Value("${app.security.password-reset.from:}") String sender) {
        this.jdbc = jdbc;
        this.users = users;
        this.attempts = attempts;
        this.encoder = encoder;
        this.passwordPolicy = passwordPolicy;
        this.mfaService = mfaService;
        this.mailer = mailer;
        this.audit = audit;
        this.enabled = enabled;
        this.frontendUrl = frontendUrl == null ? "" : frontendUrl.trim();
        this.sender = sender == null ? "" : sender.trim();
    }

    public Map<String, String> start(String email) {
        if (!enabled || !mailer.isConfigured() || !isTrustedFrontendUrl(frontendUrl) || sender.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La recuperación por correo no está configurada en este momento");
        }

        String rawEmail = email == null ? "" : email.trim();
        String normalized = rawEmail.length() > 254 ? "" : rawEmail.toLowerCase(Locale.ROOT);
        String rateKey = attempts.keyFor("password-reset:" + normalized);
        if (attempts.isBlocked(rateKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "La solicitud está temporalmente limitada. Intente más tarde.");
        }
        if (!attempts.recordFailure(rateKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "La solicitud está temporalmente limitada. Intente más tarde.");
        }
        if (normalized.isBlank() || normalized.length() > 254 || !normalized.contains("@")) {
            return Map.of("message", GENERIC_MESSAGE);
        }

        users.findByEmail(normalized).filter(user -> !RolPerfilUtil.activeRoles(user).isEmpty())
                .ifPresent(user -> issueToken(user));
        return Map.of("message", GENERIC_MESSAGE);
    }

    @Transactional
    public Map<String, String> complete(String rawToken, String newPassword, String confirmation) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La recuperación por correo no está configurada en este momento");
        }
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{40,80}")) {
            throw new BadRequestException("El enlace no es válido o ya expiró. Solicite uno nuevo.");
        }
        if (newPassword == null || !newPassword.equals(confirmation)) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }
        passwordPolicy.validate(newPassword);

        List<Map<String, Object>> tokens = jdbc.queryForList(
                "SELECT id, usuario_id, expires_at, used_at FROM password_reset_tokens "
                        + "WHERE token_hash = ? FOR UPDATE", sha256(rawToken));
        if (tokens.isEmpty()) throw invalidToken();
        Map<String, Object> token = tokens.get(0);
        Instant expiresAt = instantValue(token.get("expires_at"));
        if (token.get("used_at") != null || expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw invalidToken();
        }

        long userId = ((Number) token.get("usuario_id")).longValue();
        Usuario user = users.findById(userId).orElseThrow(PasswordResetService::invalidToken);
        if (RolPerfilUtil.activeRoles(user).isEmpty()) throw invalidToken();

        user.setPassword(encoder.encode(newPassword));
        users.save(user);
        Instant now = Instant.now();
        jdbc.update("UPDATE password_reset_tokens SET used_at = ? WHERE usuario_id = ? AND used_at IS NULL",
                Timestamp.from(now), userId);
        attempts.recordSuccess(attempts.keyFor(user.getEmail()));
        mfaService.revokeSessions(userId);
        audit.registrarEvento(user.getEmail(), "PASSWORD_RESET", "AUTENTICACION",
                "Se restableció la contraseña; las sesiones previas quedaron invalidadas");
        return Map.of("message", "La contraseña se actualizó. Inicie sesión con su nueva contraseña.");
    }

    @Scheduled(fixedDelayString = "${app.security.password-reset.cleanup-interval-ms:3600000}")
    public void removeExpiredResetTokens() {
        jdbc.update("DELETE FROM password_reset_tokens WHERE expires_at < ? OR used_at IS NOT NULL",
                Timestamp.from(Instant.now()));
    }

    private void issueToken(Usuario user) {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String tokenHash = sha256(rawToken);
        Instant now = Instant.now();
        jdbc.update("DELETE FROM password_reset_tokens WHERE usuario_id = ?", user.getId());
        jdbc.update("INSERT INTO password_reset_tokens (usuario_id, token_hash, expires_at, used_at) "
                        + "VALUES (?, ?, ?, NULL)",
                user.getId(), tokenHash, Timestamp.from(now.plusSeconds(TOKEN_TTL_SECONDS)));
        // Put the bearer token in the fragment so it is not sent in HTTP requests or Referer headers.
        String link = frontendUrl + "#resetToken=" + rawToken;
        try {
            mailer.sendResetLink(user.getEmail(), sender, link);
        } catch (RuntimeException ignored) {
            // Queue saturation or shutdown must not make the response reveal account existence.
        }
        audit.registrarEvento(user.getEmail(), "PASSWORD_RESET_REQUEST", "AUTENTICACION",
                "Se solicitó un enlace de restablecimiento de contraseña");
    }

    private static boolean isTrustedFrontendUrl(String value) {
        try {
            URI uri = URI.create(value);
            boolean https = "https".equalsIgnoreCase(uri.getScheme());
            boolean localHttp = "http".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && Set.of("localhost", "127.0.0.1", "::1").contains(uri.getHost().toLowerCase(Locale.ROOT));
            return (https || localHttp) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getFragment() == null;
        } catch (Exception exception) {
            return false;
        }
    }

    private static BadRequestException invalidToken() {
        return new BadRequestException("El enlace no es válido o ya expiró. Solicite uno nuevo.");
    }

    private static Instant instantValue(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof LocalDateTime dateTime) return dateTime.toInstant(ZoneOffset.UTC);
        if (value instanceof java.util.Date date) return date.toInstant();
        return null;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible proteger el token de recuperación", exception);
        }
    }
}
