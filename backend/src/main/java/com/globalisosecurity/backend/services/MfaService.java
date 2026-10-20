package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.security.MfaSecretCrypto;
import com.globalisosecurity.backend.security.Totp;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database-backed, single-use TOTP challenges and one-time recovery codes. */
@Service
public class MfaService {

    private static final String ISSUER = "Global ISO Security";
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration CHALLENGE_DURATION = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final MfaSecretCrypto crypto;

    public MfaService(JdbcTemplate jdbc, MfaSecretCrypto crypto) {
        this.jdbc = jdbc;
        this.crypto = crypto;
    }

    public record StartResult(boolean rateLimited, Map<String, Object> body) {}

    public record VerificationResult(
            boolean success, Long userId, boolean rateLimited, List<String> recoveryCodes) {
        static VerificationResult failed(boolean rateLimited) {
            return new VerificationResult(false, null, rateLimited, List.of());
        }
    }

    @Transactional
    public StartResult start(Usuario user) {
        long userId = user.getId();
        lockUser(userId);
        Map<String, Object> credential = getCredentialForUpdate(userId);
        if (credential == null) {
            jdbc.update("INSERT INTO usuario_mfa (usuario_id, enabled) VALUES (?, 0)", userId);
            credential = getCredentialForUpdate(userId);
        }

        Instant now = Instant.now();
        Instant lockedUntil = instantValue(credential.get("locked_until"));
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return new StartResult(true, Map.of());
        }
        if (lockedUntil != null) {
            // Clear the old counter when the cooldown has elapsed; otherwise the
            // next typo would immediately lock the account again.
            jdbc.update("UPDATE usuario_mfa SET failed_attempts = 0, locked_until = NULL, updated_at = ? "
                            + "WHERE usuario_id = ?",
                    timestamp(now), userId);
        }

        // Challenges are short-lived and single-use. Prune old rows for this
        // account so repeated sign-ins cannot grow this table indefinitely.
        jdbc.update("DELETE FROM mfa_login_challenges WHERE usuario_id = ? "
                        + "AND (consumed_at IS NOT NULL OR expires_at < ?)",
                userId, timestamp(now.minus(Duration.ofDays(1))));

        boolean enabled = numberValue(credential.get("enabled")) != 0;
        String setupSecret = null;
        String challengeType = enabled ? "LOGIN" : "SETUP";
        if (!enabled) {
            setupSecret = Totp.generateSecret();
            jdbc.update("UPDATE usuario_mfa SET pending_secret_ciphertext = ?, updated_at = ? WHERE usuario_id = ?",
                    crypto.encrypt(setupSecret), timestamp(now), userId);
        }

        jdbc.update("UPDATE mfa_login_challenges SET consumed_at = ? "
                        + "WHERE usuario_id = ? AND consumed_at IS NULL",
                timestamp(now), userId);

        String challengeToken = newChallengeToken();
        jdbc.update("INSERT INTO mfa_login_challenges "
                        + "(usuario_id, token_hash, challenge_type, expires_at, attempts) VALUES (?, ?, ?, ?, 0)",
                userId, sha256(challengeToken), challengeType, timestamp(now.plus(CHALLENGE_DURATION)));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mfaRequired", true);
        response.put("mfaSetupRequired", !enabled);
        response.put("challengeToken", challengeToken);
        response.put("expiresInSeconds", CHALLENGE_DURATION.toSeconds());
        if (setupSecret != null) {
            response.put("setupSecret", setupSecret);
            response.put("otpauthUri", otpAuthUri(user.getEmail(), setupSecret));
        }
        return new StartResult(false, response);
    }

    @Transactional
    public VerificationResult verify(String rawChallengeToken, String rawCode) {
        if (rawChallengeToken == null || !rawChallengeToken.matches("[A-Za-z0-9_-]{40,80}")
                || rawCode == null || rawCode.length() > 32) {
            return VerificationResult.failed(false);
        }

        List<Map<String, Object>> challenges = jdbc.queryForList(
                "SELECT id, usuario_id, challenge_type, expires_at, attempts, consumed_at "
                        + "FROM mfa_login_challenges WHERE token_hash = ? FOR UPDATE",
                sha256(rawChallengeToken));
        if (challenges.isEmpty()) return VerificationResult.failed(false);

        Map<String, Object> challenge = challenges.get(0);
        long challengeId = numberValue(challenge.get("id"));
        long userId = numberValue(challenge.get("usuario_id"));
        Instant now = Instant.now();
        if (challenge.get("consumed_at") != null
                || !instantValue(challenge.get("expires_at")).isAfter(now)) {
            jdbc.update("UPDATE mfa_login_challenges SET consumed_at = ? WHERE id = ?",
                    timestamp(now), challengeId);
            return VerificationResult.failed(false);
        }

        lockUser(userId);
        Map<String, Object> credential = getCredentialForUpdate(userId);
        if (credential == null) return VerificationResult.failed(false);
        Instant lockedUntil = instantValue(credential.get("locked_until"));
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return VerificationResult.failed(true);
        }

        String type = String.valueOf(challenge.get("challenge_type"));
        if (!"LOGIN".equals(type) && !"SETUP".equals(type)) {
            jdbc.update("UPDATE mfa_login_challenges SET consumed_at = ? WHERE id = ?",
                    timestamp(now), challengeId);
            return VerificationResult.failed(false);
        }
        String code = rawCode == null ? "" : rawCode.trim();
        Long acceptedStep = null;
        boolean recoveryCode = false;
        String secretCiphertext = "SETUP".equals(type)
                ? stringValue(credential.get("pending_secret_ciphertext"))
                : stringValue(credential.get("secret_ciphertext"));

        if (secretCiphertext != null && code.matches("[0-9]{6}")) {
            Long lastUsed = nullableLong(credential.get("last_used_step"));
            OptionalLong match = Totp.matchingStep(
                    crypto.decrypt(secretCiphertext), code, now.toEpochMilli(), lastUsed);
            if (match.isPresent()) acceptedStep = match.getAsLong();
        } else if ("LOGIN".equals(type)) {
            String normalizedRecovery = code.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
            if (normalizedRecovery.matches("[A-Z2-7]{16}")) {
                List<Map<String, Object>> matches = jdbc.queryForList(
                        "SELECT id FROM mfa_recovery_codes "
                                + "WHERE usuario_id = ? AND code_hash = ? AND used_at IS NULL FOR UPDATE",
                        userId, sha256(normalizedRecovery));
                if (!matches.isEmpty()) {
                    jdbc.update("UPDATE mfa_recovery_codes SET used_at = ? WHERE id = ? AND used_at IS NULL",
                            timestamp(now), numberValue(matches.get(0).get("id")));
                    recoveryCode = true;
                }
            }
        }

        if (acceptedStep == null && !recoveryCode) {
            return recordFailure(challengeId, userId, numberValue(challenge.get("attempts")),
                    numberValue(credential.get("failed_attempts")), now);
        }

        List<String> recoveryCodes = List.of();
        if ("SETUP".equals(type)) {
            if (acceptedStep == null) {
                return recordFailure(challengeId, userId, numberValue(challenge.get("attempts")),
                        numberValue(credential.get("failed_attempts")), now);
            }
            jdbc.update("UPDATE usuario_mfa SET enabled = 1, secret_ciphertext = pending_secret_ciphertext, "
                            + "pending_secret_ciphertext = NULL, last_used_step = ?, failed_attempts = 0, "
                            + "locked_until = NULL, updated_at = ? WHERE usuario_id = ?",
                    acceptedStep, timestamp(now), userId);
            recoveryCodes = issueRecoveryCodes(userId, now);
        } else if (acceptedStep != null) {
            jdbc.update("UPDATE usuario_mfa SET last_used_step = ?, failed_attempts = 0, "
                            + "locked_until = NULL, updated_at = ? WHERE usuario_id = ?",
                    acceptedStep, timestamp(now), userId);
        } else {
            jdbc.update("UPDATE usuario_mfa SET failed_attempts = 0, locked_until = NULL, updated_at = ? "
                            + "WHERE usuario_id = ?",
                    timestamp(now), userId);
        }

        jdbc.update("UPDATE mfa_login_challenges SET consumed_at = ? WHERE id = ?",
                timestamp(now), challengeId);
        return new VerificationResult(true, userId, false, recoveryCodes);
    }

    @Transactional
    public void reset(long userId) {
        Instant now = Instant.now();
        lockUser(userId);
        List<Map<String, Object>> credential = jdbc.queryForList(
                "SELECT usuario_id FROM usuario_mfa WHERE usuario_id = ? FOR UPDATE", userId);
        if (credential.isEmpty()) {
            jdbc.update("INSERT INTO usuario_mfa (usuario_id, enabled, session_epoch) VALUES (?, 0, 1)", userId);
        } else {
            jdbc.update("UPDATE usuario_mfa SET secret_ciphertext = NULL, pending_secret_ciphertext = NULL, "
                            + "enabled = 0, last_used_step = NULL, failed_attempts = 0, locked_until = NULL, "
                            + "session_epoch = session_epoch + 1, updated_at = ? WHERE usuario_id = ?",
                    timestamp(now), userId);
        }
        jdbc.update("DELETE FROM mfa_recovery_codes WHERE usuario_id = ?", userId);
        jdbc.update("UPDATE mfa_login_challenges SET consumed_at = ? "
                        + "WHERE usuario_id = ? AND consumed_at IS NULL",
                timestamp(now), userId);
    }

    private VerificationResult recordFailure(
            long challengeId, long userId, long challengeFailures, long userFailures, Instant now) {
        int nextChallengeFailures = (int) challengeFailures + 1;
        int nextUserFailures = (int) userFailures + 1;
        boolean lock = nextUserFailures >= MAX_FAILURES;
        Instant blockedUntil = lock ? now.plus(LOCK_DURATION) : null;
        jdbc.update("UPDATE mfa_login_challenges SET attempts = ?, consumed_at = ? WHERE id = ?",
                nextChallengeFailures,
                nextChallengeFailures >= MAX_FAILURES || lock ? timestamp(now) : null,
                challengeId);
        jdbc.update("UPDATE usuario_mfa SET failed_attempts = ?, locked_until = ?, updated_at = ? "
                        + "WHERE usuario_id = ?",
                nextUserFailures, blockedUntil == null ? null : timestamp(blockedUntil), timestamp(now), userId);
        if (lock) {
            jdbc.update("UPDATE mfa_login_challenges SET consumed_at = ? "
                            + "WHERE usuario_id = ? AND consumed_at IS NULL",
                    timestamp(now), userId);
        }
        return VerificationResult.failed(lock);
    }

    private List<String> issueRecoveryCodes(long userId, Instant now) {
        List<String> codes = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            byte[] bytes = new byte[10];
            RANDOM.nextBytes(bytes);
            String raw = Totp.base32Encode(bytes);
            String formatted = raw.substring(0, 4) + "-" + raw.substring(4, 8)
                    + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16);
            jdbc.update("INSERT INTO mfa_recovery_codes (usuario_id, code_hash, created_at) VALUES (?, ?, ?)",
                    userId, sha256(raw), timestamp(now));
            codes.add(formatted);
        }
        return List.copyOf(codes);
    }

    private Map<String, Object> getCredentialForUpdate(long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT enabled, secret_ciphertext, pending_secret_ciphertext, last_used_step, "
                        + "failed_attempts, locked_until FROM usuario_mfa WHERE usuario_id = ? FOR UPDATE", userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public long sessionVersionForUserId(long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT session_epoch FROM usuario_mfa WHERE usuario_id = ?", userId);
        return rows.isEmpty() ? 0L : numberValue(rows.get(0).get("session_epoch"));
    }

    public boolean isSessionCurrent(String email, long tokenVersion) {
        List<Long> versions = jdbc.query(
                "SELECT COALESCE(m.session_epoch, 0) FROM usuarios u "
                        + "LEFT JOIN usuario_mfa m ON m.usuario_id = u.id WHERE LOWER(u.email) = ?",
                (resultSet, rowNum) -> resultSet.getLong(1),
                email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        return !versions.isEmpty() && versions.get(0) == tokenVersion;
    }

    private void lockUser(long userId) {
        jdbc.queryForObject("SELECT id FROM usuarios WHERE id = ? FOR UPDATE", Long.class, userId);
    }

    private static String otpAuthUri(String email, String secret) {
        String account = email == null || email.isBlank() ? "usuario" : email;
        String label = urlEncode(ISSUER + ":" + account);
        return "otpauth://totp/" + label + "?secret=" + secret
                + "&issuer=" + urlEncode(ISSUER) + "&algorithm=SHA1&digits=6&period=30";
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String newChallengeToken() {
        byte[] token = new byte[32];
        RANDOM.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible calcular el hash de control", ex);
        }
    }

    private static long numberValue(Object value) {
        if (value instanceof Boolean enabled) return enabled ? 1L : 0L;
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Instant instantValue(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof java.time.LocalDateTime dateTime) return dateTime.toInstant(java.time.ZoneOffset.UTC);
        if (value instanceof java.util.Date date) return date.toInstant();
        throw new IllegalStateException("Tipo de fecha MFA no soportado");
    }

    private static java.sql.Timestamp timestamp(Instant value) {
        return java.sql.Timestamp.from(value);
    }
}
