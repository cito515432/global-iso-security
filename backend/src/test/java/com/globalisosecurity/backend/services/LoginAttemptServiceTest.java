package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterConfiguredMinimumAndClearsOnSuccess() {
        LoginAttemptService service = service(new JdbcTemplate(dataSource()));

        assertTrue(service.recordFailure("user|ip"));
        assertTrue(service.recordFailure("user|ip"));
        assertTrue(service.recordFailure("user|ip"));
        assertTrue(service.isBlocked("user|ip"));

        service.recordSuccess("user|ip");
        assertFalse(service.isBlocked("user|ip"));
    }

    @Test
    void sharesLockoutStateAcrossServiceInstances() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource());
        LoginAttemptService first = service(jdbc);
        LoginAttemptService second = service(jdbc);

        assertTrue(first.recordFailure("same-account"));
        assertTrue(first.recordFailure("same-account"));
        assertTrue(second.recordFailure("same-account"));
        assertTrue(second.isBlocked("same-account"));
        assertFalse(first.recordFailure("same-account"));
    }

    private static LoginAttemptService service(JdbcTemplate jdbc) {
        return new LoginAttemptService(jdbc, 3, 15, 15);
    }

    private static DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:login_attempts_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new JdbcTemplate(dataSource).execute("CREATE TABLE login_attempt_buckets ("
                + "bucket_key CHAR(64) PRIMARY KEY, failures INT NOT NULL DEFAULT 0, "
                + "window_started_at TIMESTAMP NOT NULL, blocked_until TIMESTAMP NULL, "
                + "updated_at TIMESTAMP NOT NULL)");
        return dataSource;
    }
}
