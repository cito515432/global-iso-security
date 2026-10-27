-- Mirror of backend Flyway V3__server_side_idle_sessions.sql.
CREATE TABLE auth_sessions (
    session_id CHAR(36) NOT NULL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    last_activity_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_auth_sessions_user_activity ON auth_sessions (usuario_id, last_activity_at);
