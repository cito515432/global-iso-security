package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.utils.RequestUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Limitador de intentos de inicio de sesión en memoria. Reduce ataques de fuerza
 * bruta sin introducir una dependencia externa. En un despliegue horizontal debe
 * sustituirse por un almacén compartido (por ejemplo Redis).
 */
@Service
public class LoginAttemptService {

    private record AttemptState(int failures, Instant windowStartedAt, Instant blockedUntil) {}

    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final int maxTrackedKeys;
    private final Duration window;
    private final Duration blockDuration;
    private long lastCapacitySweepMillis;

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.window-minutes:15}") long windowMinutes,
            @Value("${app.security.login.block-minutes:15}") long blockMinutes,
            @Value("${app.security.login.max-tracked-keys:10000}") int maxTrackedKeys) {
        this.maxAttempts = Math.max(3, maxAttempts);
        this.maxTrackedKeys = Math.max(1, maxTrackedKeys);
        this.window = Duration.ofMinutes(Math.max(1, windowMinutes));
        this.blockDuration = Duration.ofMinutes(Math.max(1, blockMinutes));
    }

    public String keyFor(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return normalized + "|" + RequestUtils.getClientIp();
    }

    public boolean isBlocked(String key) {
        AttemptState state = attempts.get(key);
        if (state == null || state.blockedUntil() == null) return false;
        if (state.blockedUntil().isAfter(Instant.now())) return true;
        attempts.remove(key, state);
        return false;
    }

    public long remainingSeconds(String key) {
        AttemptState state = attempts.get(key);
        if (state == null || state.blockedUntil() == null) return 0L;
        return Math.max(0L, Duration.between(Instant.now(), state.blockedUntil()).toSeconds());
    }

    /** Returns false when the bounded in-memory store is full and this key cannot be tracked. */
    public synchronized boolean recordFailure(String key) {
        Instant now = Instant.now();
        if (!attempts.containsKey(key) && attempts.size() >= maxTrackedKeys) {
            long nowMillis = now.toEpochMilli();
            if (nowMillis - lastCapacitySweepMillis >= 60_000L) {
                attempts.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
                lastCapacitySweepMillis = nowMillis;
            }
            if (attempts.size() >= maxTrackedKeys) return false;
        }
        attempts.compute(key, (ignored, old) -> {
            if (old == null || old.windowStartedAt().plus(window).isBefore(now)) {
                return new AttemptState(1, now, null);
            }
            if (old.blockedUntil() != null && old.blockedUntil().isAfter(now)) {
                return old;
            }
            int failures = old.failures() + 1;
            Instant blockedUntil = failures >= maxAttempts ? now.plus(blockDuration) : null;
            return new AttemptState(failures, old.windowStartedAt(), blockedUntil);
        });
        return true;
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(AttemptState state, Instant now) {
        Instant expiry = state.blockedUntil() == null
                ? state.windowStartedAt().plus(window)
                : state.blockedUntil();
        return !expiry.isAfter(now);
    }
}
