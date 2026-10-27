-- Mirror of backend Flyway V4__multiple_roles_per_user.sql.
CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    INDEX idx_usuario_roles_role (rol_id, usuario_id)
);

