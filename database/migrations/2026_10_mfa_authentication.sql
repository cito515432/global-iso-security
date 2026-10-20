-- Mirrored from backend/src/main/resources/db/migration/V1__mfa_authentication.sql.
-- Additive authentication-only schema. No business or SoA records are changed.
CREATE TABLE usuario_mfa (
    usuario_id BIGINT NOT NULL PRIMARY KEY,
    secret_ciphertext VARCHAR(512) NULL,
    pending_secret_ciphertext VARCHAR(512) NULL,
    enabled TINYINT NOT NULL DEFAULT 0,
    last_used_step BIGINT NULL,
    session_epoch BIGINT NOT NULL DEFAULT 0,
    failed_attempts SMALLINT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mfa_login_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    challenge_type VARCHAR(16) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    attempts TINYINT NOT NULL DEFAULT 0,
    consumed_at DATETIME(6) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mfa_login_challenge_token UNIQUE (token_hash)
);

CREATE INDEX idx_mfa_login_challenge_user_expiry ON mfa_login_challenges (usuario_id, expires_at);

CREATE TABLE mfa_recovery_codes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    code_hash CHAR(64) NOT NULL,
    used_at DATETIME(6) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mfa_recovery_user_hash UNIQUE (usuario_id, code_hash)
);

CREATE INDEX idx_mfa_recovery_user_unused ON mfa_recovery_codes (usuario_id, used_at);
