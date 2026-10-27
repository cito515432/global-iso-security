-- Keep rol_id as the primary role for compatibility while adding an assignable role set.
CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    INDEX idx_usuario_roles_role (rol_id, usuario_id)
);
