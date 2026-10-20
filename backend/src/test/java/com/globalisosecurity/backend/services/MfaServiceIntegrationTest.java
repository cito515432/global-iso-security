package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.security.Totp;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mfa_integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false",
        "jwt.secret=mfa-test-secret-with-at-least-32-characters",
        "app.seed.enabled=false",
        "app.seed.demo-data=false",
        "app.rpm.ml.enabled=false",
        "app.security.mfa.encryption-key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
        "spring.flyway.enabled=true"
})
class MfaServiceIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MfaService mfaService;

    private final Usuario user = new Usuario();

    @BeforeEach
    void prepareUserAndMfaTables() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS usuarios (id BIGINT PRIMARY KEY, email VARCHAR(255) NOT NULL)");
        jdbc.update("DELETE FROM mfa_recovery_codes");
        jdbc.update("DELETE FROM mfa_login_challenges");
        jdbc.update("DELETE FROM usuario_mfa");
        jdbc.update("DELETE FROM usuarios");
        jdbc.update("INSERT INTO usuarios (id, email) VALUES (?, ?)", 9001L, "empresa@example.test");
        user.setId(9001L);
        user.setEmail("empresa@example.test");
    }

    @Test
    void firstLoginMustEnrollAndProducesOneTimeRecoveryCodes() {
        MfaService.StartResult start = mfaService.start(user);
        assertFalse(start.rateLimited());
        Map<String, Object> body = start.body();
        assertTrue(Boolean.TRUE.equals(body.get("mfaSetupRequired")));

        String secret = String.valueOf(body.get("setupSecret"));
        String token = String.valueOf(body.get("challengeToken"));
        MfaService.VerificationResult enrollment = mfaService.verify(
                token, Totp.codeFor(secret, System.currentTimeMillis()));
        assertTrue(enrollment.success());
        assertTrue(enrollment.recoveryCodes().size() == 10);

        MfaService.StartResult nextLogin = mfaService.start(user);
        assertFalse(Boolean.TRUE.equals(nextLogin.body().get("mfaSetupRequired")));
        String nextToken = String.valueOf(nextLogin.body().get("challengeToken"));
        MfaService.VerificationResult completed = mfaService.verify(
                nextToken, Totp.codeFor(secret, System.currentTimeMillis() + 30_000L));
        assertTrue(completed.success());
        assertTrue(completed.recoveryCodes().isEmpty());

        String recoveryCode = enrollment.recoveryCodes().get(0);
        String recoveryToken = String.valueOf(mfaService.start(user).body().get("challengeToken"));
        assertTrue(mfaService.verify(recoveryToken, recoveryCode).success());
        String replayToken = String.valueOf(mfaService.start(user).body().get("challengeToken"));
        assertFalse(mfaService.verify(replayToken, recoveryCode).success());
    }

    @Test
    void recoveryChallengeIsSeparateFromNormalLoginChallenges() {
        MfaService.StartResult setup = mfaService.start(user);
        String secret = String.valueOf(setup.body().get("setupSecret"));
        String setupToken = String.valueOf(setup.body().get("challengeToken"));
        assertTrue(mfaService.verify(
                setupToken, Totp.codeFor(secret, System.currentTimeMillis())).success());

        MfaService.StartResult recovery = mfaService.startRecovery(user);
        assertFalse(recovery.rateLimited());
        assertTrue(Boolean.TRUE.equals(recovery.body().get("mfaRequired")));
        String recoveryToken = String.valueOf(recovery.body().get("challengeToken"));
        assertTrue(mfaService.verifyRecovery(
                recoveryToken, Totp.codeFor(secret, System.currentTimeMillis() + 30_000L)).success());
    }

    @Test
    void challengeIsOneUseAndMfaFailureLocksTheAccount() {
        MfaService.StartResult setup = mfaService.start(user);
        String secret = String.valueOf(setup.body().get("setupSecret"));
        String setupToken = String.valueOf(setup.body().get("challengeToken"));
        assertTrue(mfaService.verify(setupToken, Totp.codeFor(secret, System.currentTimeMillis())).success());

        String currentCode = Totp.codeFor(secret, System.currentTimeMillis() + 30_000L);
        MfaService.StartResult login = mfaService.start(user);
        String loginToken = String.valueOf(login.body().get("challengeToken"));
        assertTrue(mfaService.verify(loginToken, currentCode).success());
        assertFalse(mfaService.verify(loginToken, currentCode).success());

        String invalidCode = invalidCode(secret);
        for (int attempt = 0; attempt < 5; attempt++) {
            MfaService.StartResult next = mfaService.start(user);
            String token = String.valueOf(next.body().get("challengeToken"));
            MfaService.VerificationResult failure = mfaService.verify(token, invalidCode);
            if (attempt == 4) assertTrue(failure.rateLimited());
        }
        assertTrue(mfaService.start(user).rateLimited());
    }

    private static String invalidCode(String secret) {
        long now = System.currentTimeMillis();
        long currentStep = Math.floorDiv(now, 30_000L);
        java.util.Set<String> acceptedCodes = new java.util.HashSet<>();
        for (long step = currentStep - 1; step <= currentStep + 1; step++) {
            acceptedCodes.add(Totp.codeFor(secret, step * 30_000L));
        }
        for (int candidate = 0; candidate < 1_000_000; candidate++) {
            String code = String.format("%06d", candidate);
            if (!acceptedCodes.contains(code)) return code;
        }
        throw new IllegalStateException("Could not choose a non-matching test code");
    }
}
