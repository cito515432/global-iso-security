-- Agrega estado, descripción y permisos configurables a roles.
-- Ejecutar sobre la base globalisosecurity si la aplicación no usa spring.jpa.hibernate.ddl-auto=update.

ALTER TABLE roles ADD COLUMN descripcion VARCHAR(500) NULL;
ALTER TABLE roles ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE roles ADD COLUMN permisos TEXT NULL;

UPDATE roles
SET descripcion = CASE
    WHEN UPPER(nombre) LIKE '%ADMIN%' THEN 'Acceso completo al panel administrativo.'
    WHEN UPPER(nombre) LIKE '%IMPLEMENTADOR%' THEN 'Gestiona procesos de implementación y empresas asignadas.'
    WHEN UPPER(nombre) LIKE '%AUDITOR%' THEN 'Revisa auditorías, evidencias y reportes.'
    WHEN UPPER(nombre) LIKE '%CAPACITADOR%' THEN 'Gestiona actividades de capacitación.'
    ELSE 'Acceso limitado de consulta.'
END,
activo = 1
WHERE descripcion IS NULL;
