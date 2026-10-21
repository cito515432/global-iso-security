-- Additive controls for persistent login throttling and one-time password reset.
-- No business, user, service, SoA, risk, or evidence rows are modified.
CREATE TABLE login_attempt_buckets (
    bucket_key CHAR(64) NOT NULL PRIMARY KEY,
    failures INT NOT NULL DEFAULT 0,
    window_started_at DATETIME(6) NOT NULL,
    blocked_until DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_login_attempt_buckets_updated ON login_attempt_buckets (updated_at);

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_user_expiry ON password_reset_tokens (usuario_id, expires_at);
