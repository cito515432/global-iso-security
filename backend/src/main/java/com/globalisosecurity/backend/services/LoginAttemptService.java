package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.utils.RequestUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database-backed login throttling shared by app instances and restarts. */
@Service
public class LoginAttemptService {

    private final JdbcTemplate jdbc;
    private final int maxAttempts;
    private final Duration window;
    private final Duration blockDuration;

    public LoginAttemptService(JdbcTemplate jdbc,
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.window-minutes:15}") long windowMinutes,
            @Value("${app.security.login.block-minutes:15}") long blockMinutes) {
        this.jdbc = jdbc;
        this.maxAttempts = Math.max(3, maxAttempts);
        this.window = Duration.ofMinutes(Math.max(1, windowMinutes));
        this.blockDuration = Duration.ofMinutes(Math.max(1, blockMinutes));
    }

    /** Stores only a digest of the normalized identifier and client address. */
    public String keyFor(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        // Do not key throttling from X-Forwarded-For: a direct caller may be able to spoof it.
        return sha256(normalized + "|" + RequestUtils.getConnectionAddress());
    }

    public boolean isBlocked(String key) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT blocked_until FROM login_attempt_buckets WHERE bucket_key = ?", key);
        if (rows.isEmpty()) return false;
        Instant blockedUntil = asInstant(rows.get(0).get("blocked_until"));
        return blockedUntil != null && blockedUntil.isAfter(Instant.now());
    }

    /** Returns false when this bucket is already locked; database errors fail closed. */
    @Transactional
    public boolean recordFailure(String key) {
        Instant now = Instant.now();
        try {
            jdbc.update("INSERT INTO login_attempt_buckets "
                            + "(bucket_key, failures, window_started_at, blocked_until, updated_at) "
                            + "VALUES (?, 0, ?, NULL, ?)",
                    key, timestamp(now), timestamp(now));
        } catch (DuplicateKeyException alreadyExists) {
            // The row is locked below; simultaneous requests then increment it serially.
        }

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT failures, window_started_at, blocked_until "
                        + "FROM login_attempt_buckets WHERE bucket_key = ? FOR UPDATE", key);
        if (rows.isEmpty()) return false;

        Map<String, Object> row = rows.get(0);
        int failures = ((Number) row.get("failures")).intValue();
        Instant windowStarted = asInstant(row.get("window_started_at"));
        Instant blockedUntil = asInstant(row.get("blocked_until"));
        if (blockedUntil != null && blockedUntil.isAfter(now)) return false;

        if (windowStarted == null || !windowStarted.plus(window).isAfter(now)) {
            failures = 0;
            windowStarted = now;
        }
        failures++;
        Instant nextBlockedUntil = failures >= maxAttempts ? now.plus(blockDuration) : null;
        jdbc.update("UPDATE login_attempt_buckets SET failures = ?, window_started_at = ?, "
                        + "blocked_until = ?, updated_at = ? WHERE bucket_key = ?",
                failures, timestamp(windowStarted), timestamp(nextBlockedUntil), timestamp(now), key);
        return true;
    }

    @Transactional
    public void recordSuccess(String key) {
        jdbc.update("DELETE FROM login_attempt_buckets WHERE bucket_key = ?", key);
    }

    /** Remove old digests periodically so the rate-limit table remains bounded over time. */
    @Scheduled(fixedDelayString = "${app.security.login.cleanup-interval-ms:3600000}")
    public void removeExpiredBuckets() {
        jdbc.update("DELETE FROM login_attempt_buckets WHERE updated_at < ?",
                timestamp(Instant.now().minus(Duration.ofDays(2))));
    }

    private static java.sql.Timestamp timestamp(Instant value) {
        return value == null ? null : java.sql.Timestamp.from(value);
    }

    private static Instant asInstant(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof LocalDateTime dateTime) return dateTime.toInstant(ZoneOffset.UTC);
        if (value instanceof java.util.Date date) return date.toInstant();
        return null;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible calcular el identificador del límite", exception);
        }
    }
}
