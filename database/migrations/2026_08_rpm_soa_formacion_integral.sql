-- Migración integral Global ISO Security: SoA, riesgos, evidencias, RPM, portal empresa y formación.
-- Compatible con MySQL 5.7/8.0. Las descripciones del catálogo son paráfrasis de trabajo; no reproducen el texto oficial de ISO.
USE globalisosecurity;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELIMITER $$
DROP PROCEDURE IF EXISTS add_column_if_missing$$
CREATE PROCEDURE add_column_if_missing(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_definition);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$

DROP PROCEDURE IF EXISTS add_index_if_missing$$
CREATE PROCEDURE add_index_if_missing(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_definition TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_definition);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$

DROP PROCEDURE IF EXISTS add_fk_if_missing$$
CREATE PROCEDURE add_fk_if_missing(IN p_table VARCHAR(64), IN p_constraint VARCHAR(64), IN p_definition TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = p_table AND constraint_name = p_constraint AND constraint_type = 'FOREIGN KEY') THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_constraint, '` ', p_definition);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- Evolución de tablas existentes.
CALL add_column_if_missing('roles', 'descripcion', '`descripcion` VARCHAR(500) NULL');
CALL add_column_if_missing('roles', 'activo', '`activo` TINYINT(1) NOT NULL DEFAULT 1');
CALL add_column_if_missing('roles', 'permisos', '`permisos` TEXT NULL');
CALL add_column_if_missing('capacitaciones', 'objetivo', '`objetivo` VARCHAR(2000) NULL');
CALL add_column_if_missing('capacitaciones', 'fecha_inicio', '`fecha_inicio` DATETIME(6) NULL');
CALL add_column_if_missing('capacitaciones', 'fecha_limite', '`fecha_limite` DATETIME(6) NULL');
CALL add_column_if_missing('capacitaciones', 'puntaje_minimo', '`puntaje_minimo` INT NOT NULL DEFAULT 80');
CALL add_column_if_missing('capacitaciones', 'publico_objetivo', '`publico_objetivo` VARCHAR(1000) NULL');
CALL add_column_if_missing('capacitaciones', 'creada_por_rpm', '`creada_por_rpm` TINYINT(1) NOT NULL DEFAULT 0');
CALL add_column_if_missing('capacitaciones', 'motivo_rpm', '`motivo_rpm` VARCHAR(2000) NULL');
CALL add_column_if_missing('capacitaciones', 'control_codigo', '`control_codigo` VARCHAR(30) NULL');
CALL add_column_if_missing('capacitaciones', 'riesgo_id_referencia', '`riesgo_id_referencia` BIGINT NULL');
CALL add_column_if_missing('constancias_capacitacion', 'codigo_verificacion', '`codigo_verificacion` VARCHAR(80) NULL');
CALL add_column_if_missing('constancias_capacitacion', 'puntaje', '`puntaje` DOUBLE NULL');
CALL add_column_if_missing('constancias_capacitacion', 'estado', '`estado` VARCHAR(30) NOT NULL DEFAULT ''VIGENTE''');
CALL add_column_if_missing('constancias_capacitacion', 'participante_id', '`participante_id` BIGINT NULL');

UPDATE constancias_capacitacion SET codigo_verificacion = CONCAT('LEGACY-', id, '-', UPPER(SUBSTRING(MD5(CONCAT(id, nombre_completo, documento)), 1, 16))) WHERE codigo_verificacion IS NULL OR codigo_verificacion = '';
ALTER TABLE constancias_capacitacion MODIFY COLUMN codigo_verificacion VARCHAR(80) NOT NULL;
CALL add_index_if_missing('constancias_capacitacion', 'uk_constancia_codigo', 'UNIQUE INDEX `uk_constancia_codigo` (`codigo_verificacion`)');

-- Catálogo maestro y contexto organizacional.
CREATE TABLE IF NOT EXISTS catalogo_controles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  codigo VARCHAR(20) NOT NULL,
  dominio VARCHAR(60) NOT NULL,
  titulo VARCHAR(500) NOT NULL,
  descripcion VARCHAR(3000) NULL,
  pregunta_evaluacion VARCHAR(3000) NULL,
  etiquetas VARCHAR(1000) NULL,
  version_norma VARCHAR(50) NOT NULL DEFAULT 'ISO/IEC 27001:2022',
  activo TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_catalogo_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS perfiles_organizacionales (
  id BIGINT NOT NULL AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  sector_id BIGINT NULL,
  tamano VARCHAR(255) NOT NULL DEFAULT 'PEQUENA',
  maneja_datos_sensibles TINYINT(1) NOT NULL DEFAULT 0,
  usa_servicios_nube TINYINT(1) NOT NULL DEFAULT 0,
  permite_trabajo_remoto TINYINT(1) NOT NULL DEFAULT 0,
  procesa_pagos TINYINT(1) NOT NULL DEFAULT 0,
  infraestructura_propia TINYINT(1) NOT NULL DEFAULT 0,
  depende_proveedores TINYINT(1) NOT NULL DEFAULT 0,
  servicio_critico_24x7 TINYINT(1) NOT NULL DEFAULT 0,
  maneja_menores TINYINT(1) NOT NULL DEFAULT 0,
  opera_ot_iot TINYINT(1) NOT NULL DEFAULT 0,
  alcance_sgsi VARCHAR(3000) NULL,
  responsable_sgsi VARCHAR(255) NULL,
  umbral_aceptacion INT NOT NULL DEFAULT 8,
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_perfil_empresa (empresa_id),
  KEY idx_perfil_sector (sector_id),
  CONSTRAINT fk_perfil_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
  CONSTRAINT fk_perfil_sector FOREIGN KEY (sector_id) REFERENCES sectores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soa_controles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  servicio_id BIGINT NOT NULL,
  control_id BIGINT NOT NULL,
  aplicabilidad VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  justificacion_aplicabilidad VARCHAR(3000) NULL,
  estado_implementacion VARCHAR(30) NOT NULL DEFAULT 'NO_INICIADO',
  porcentaje_implementacion INT NOT NULL DEFAULT 0,
  responsable VARCHAR(255) NULL,
  fecha_objetivo DATE NULL,
  observaciones VARCHAR(3000) NULL,
  recomendacion_contextual VARCHAR(1500) NULL,
  puntaje_relevancia INT NOT NULL DEFAULT 0,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_soa_servicio_control (servicio_id, control_id),
  KEY idx_soa_control (control_id),
  CONSTRAINT fk_soa_servicio FOREIGN KEY (servicio_id) REFERENCES servicios(id),
  CONSTRAINT fk_soa_control FOREIGN KEY (control_id) REFERENCES catalogo_controles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Riesgos y relación formal con los controles.
CREATE TABLE IF NOT EXISTS riesgos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  servicio_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nombre VARCHAR(500) NOT NULL,
  activo_informacion VARCHAR(1000) NULL,
  amenaza VARCHAR(1500) NULL,
  vulnerabilidad VARCHAR(1500) NULL,
  consecuencia VARCHAR(2000) NULL,
  probabilidad INT NOT NULL DEFAULT 1,
  impacto INT NOT NULL DEFAULT 1,
  nivel_inherente INT NOT NULL DEFAULT 1,
  nivel_residual INT NULL,
  tratamiento VARCHAR(50) NULL DEFAULT 'MITIGAR',
  responsable VARCHAR(255) NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTO',
  fecha_revision DATE NULL,
  descripcion VARCHAR(3000) NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_riesgo_servicio_codigo (servicio_id, codigo),
  CONSTRAINT fk_riesgo_servicio FOREIGN KEY (servicio_id) REFERENCES servicios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS riesgos_controles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  riesgo_id BIGINT NOT NULL,
  control_id BIGINT NOT NULL,
  tipo_relacion VARCHAR(40) NOT NULL DEFAULT 'TRATAMIENTO',
  eficacia_esperada INT NOT NULL DEFAULT 50,
  observacion VARCHAR(1500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_riesgo_control (riesgo_id, control_id),
  KEY idx_rc_control (control_id),
  CONSTRAINT fk_rc_riesgo FOREIGN KEY (riesgo_id) REFERENCES riesgos(id),
  CONSTRAINT fk_rc_control FOREIGN KEY (control_id) REFERENCES catalogo_controles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Evidencias y hallazgos auditables.
CREATE TABLE IF NOT EXISTS evidencias (
  id BIGINT NOT NULL AUTO_INCREMENT,
  servicio_id BIGINT NOT NULL,
  soa_control_id BIGINT NOT NULL,
  nombre_original VARCHAR(500) NOT NULL,
  nombre_almacenado VARCHAR(500) NOT NULL,
  ruta_archivo VARCHAR(1500) NOT NULL,
  tipo_mime VARCHAR(255) NULL,
  hash_sha256 VARCHAR(64) NOT NULL,
  descripcion VARCHAR(2000) NULL,
  tipo_evidencia VARCHAR(80) NULL,
  fecha_carga DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  fecha_vencimiento DATE NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  cargada_por VARCHAR(255) NOT NULL,
  validada_por VARCHAR(255) NULL,
  fecha_validacion DATETIME(6) NULL,
  observacion_validacion VARCHAR(2000) NULL,
  PRIMARY KEY (id),
  KEY idx_evidencia_servicio (servicio_id),
  KEY idx_evidencia_soa (soa_control_id),
  CONSTRAINT fk_evidencia_servicio FOREIGN KEY (servicio_id) REFERENCES servicios(id),
  CONSTRAINT fk_evidencia_soa FOREIGN KEY (soa_control_id) REFERENCES soa_controles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hallazgos_auditoria (
  id BIGINT NOT NULL AUTO_INCREMENT,
  servicio_id BIGINT NOT NULL,
  soa_control_id BIGINT NULL,
  riesgo_id BIGINT NULL,
  titulo VARCHAR(500) NOT NULL,
  descripcion VARCHAR(3000) NOT NULL,
  severidad VARCHAR(30) NOT NULL DEFAULT 'MEDIA',
  estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTO',
  recurrente TINYINT(1) NOT NULL DEFAULT 0,
  fecha_deteccion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  fecha_cierre DATETIME(6) NULL,
  creado_por VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_hallazgo_servicio (servicio_id),
  KEY idx_hallazgo_soa (soa_control_id),
  KEY idx_hallazgo_riesgo (riesgo_id),
  CONSTRAINT fk_hallazgo_servicio FOREIGN KEY (servicio_id) REFERENCES servicios(id),
  CONSTRAINT fk_hallazgo_soa FOREIGN KEY (soa_control_id) REFERENCES soa_controles(id),
  CONSTRAINT fk_hallazgo_riesgo FOREIGN KEY (riesgo_id) REFERENCES riesgos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Formación completa: módulos, participantes, banco de preguntas e intentos.
CREATE TABLE IF NOT EXISTS modulos_capacitacion (
  id BIGINT NOT NULL AUTO_INCREMENT,
  capacitacion_id BIGINT NOT NULL,
  titulo VARCHAR(500) NOT NULL,
  descripcion VARCHAR(2000) NULL,
  contenido LONGTEXT NULL,
  material_url VARCHAR(1000) NULL,
  video_url VARCHAR(1000) NULL,
  orden_modulo INT NOT NULL DEFAULT 1,
  duracion_minutos INT NOT NULL DEFAULT 15,
  obligatorio TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_modulo_capacitacion (capacitacion_id),
  CONSTRAINT fk_modulo_capacitacion FOREIGN KEY (capacitacion_id) REFERENCES capacitaciones(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS participantes_capacitacion (
  id BIGINT NOT NULL AUTO_INCREMENT,
  capacitacion_id BIGINT NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  documento VARCHAR(255) NULL,
  cargo VARCHAR(255) NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'ASIGNADO',
  progreso_porcentaje INT NOT NULL DEFAULT 0,
  puntaje_evaluacion DOUBLE NULL,
  intentos INT NOT NULL DEFAULT 0,
  fecha_asignacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  fecha_finalizacion DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_participante_capacitacion_email (capacitacion_id, email),
  CONSTRAINT fk_participante_capacitacion FOREIGN KEY (capacitacion_id) REFERENCES capacitaciones(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS preguntas_capacitacion (
  id BIGINT NOT NULL AUTO_INCREMENT,
  capacitacion_id BIGINT NOT NULL,
  enunciado VARCHAR(1200) NOT NULL,
  opcion_a VARCHAR(700) NOT NULL,
  opcion_b VARCHAR(700) NOT NULL,
  opcion_c VARCHAR(700) NULL,
  opcion_d VARCHAR(700) NULL,
  respuesta_correcta VARCHAR(1) NOT NULL,
  explicacion VARCHAR(1500) NULL,
  puntos INT NOT NULL DEFAULT 1,
  orden_pregunta INT NOT NULL DEFAULT 1,
  activa TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_pregunta_capacitacion (capacitacion_id),
  CONSTRAINT fk_pregunta_capacitacion FOREIGN KEY (capacitacion_id) REFERENCES capacitaciones(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intentos_capacitacion (
  id BIGINT NOT NULL AUTO_INCREMENT,
  participante_id BIGINT NOT NULL,
  fecha_intento DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  puntaje DOUBLE NOT NULL,
  aprobado TINYINT(1) NOT NULL,
  respuestas_json LONGTEXT NULL,
  respuestas_correctas INT NOT NULL DEFAULT 0,
  total_preguntas INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_intento_participante (participante_id),
  CONSTRAINT fk_intento_participante FOREIGN KEY (participante_id) REFERENCES participantes_capacitacion(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL add_index_if_missing('constancias_capacitacion', 'uk_constancia_participante', 'UNIQUE INDEX `uk_constancia_participante` (`participante_id`)');
CALL add_fk_if_missing('constancias_capacitacion', 'fk_constancia_participante', 'FOREIGN KEY (`participante_id`) REFERENCES `participantes_capacitacion` (`id`)');

-- Motor RPM determinista, explicable, validación humana y memoria inmunológica.
CREATE TABLE IF NOT EXISTS rpm_analisis (
  id BIGINT NOT NULL AUTO_INCREMENT,
  servicio_id BIGINT NOT NULL,
  soa_control_id BIGINT NULL,
  riesgo_id BIGINT NULL,
  generado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  puntaje INT NOT NULL DEFAULT 0,
  prioridad VARCHAR(20) NOT NULL DEFAULT 'BAJA',
  estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
  resumen VARCHAR(1000) NULL,
  explicacion VARCHAR(5000) NULL,
  version_motor VARCHAR(40) NOT NULL DEFAULT 'RPM-DETERMINISTA-1.0',
  huella_entrada VARCHAR(64) NULL,
  PRIMARY KEY (id),
  KEY idx_rpm_servicio (servicio_id),
  KEY idx_rpm_soa (soa_control_id),
  KEY idx_rpm_riesgo (riesgo_id),
  KEY idx_rpm_huella (huella_entrada),
  CONSTRAINT fk_rpm_servicio FOREIGN KEY (servicio_id) REFERENCES servicios(id),
  CONSTRAINT fk_rpm_soa FOREIGN KEY (soa_control_id) REFERENCES soa_controles(id),
  CONSTRAINT fk_rpm_riesgo FOREIGN KEY (riesgo_id) REFERENCES riesgos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rpm_senales (
  id BIGINT NOT NULL AUTO_INCREMENT,
  analisis_id BIGINT NOT NULL,
  categoria VARCHAR(30) NOT NULL,
  codigo VARCHAR(80) NOT NULL,
  descripcion VARCHAR(1500) NOT NULL,
  fuente VARCHAR(500) NULL,
  peso INT NOT NULL,
  valor VARCHAR(500) NULL,
  activa TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_rpm_senal_analisis (analisis_id),
  CONSTRAINT fk_rpm_senal_analisis FOREIGN KEY (analisis_id) REFERENCES rpm_analisis(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rpm_decisiones (
  id BIGINT NOT NULL AUTO_INCREMENT,
  analisis_id BIGINT NOT NULL,
  tipo_accion VARCHAR(50) NOT NULL,
  accion VARCHAR(3000) NOT NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  validada_por VARCHAR(255) NULL,
  fecha_validacion DATETIME(6) NULL,
  justificacion VARCHAR(3000) NULL,
  fecha_objetivo DATE NULL,
  PRIMARY KEY (id),
  KEY idx_rpm_decision_analisis (analisis_id),
  CONSTRAINT fk_rpm_decision_analisis FOREIGN KEY (analisis_id) REFERENCES rpm_analisis(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS rpm_memoria (
  id BIGINT NOT NULL AUTO_INCREMENT,
  analisis_id BIGINT NOT NULL,
  huella VARCHAR(64) NOT NULL,
  situacion_json LONGTEXT NULL,
  prioridad_inicial VARCHAR(20) NULL,
  prioridad_final VARCHAR(20) NULL,
  accion VARCHAR(3000) NULL,
  resultado VARCHAR(3000) NULL,
  efectividad_porcentaje INT NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_rpm_memoria_analisis (analisis_id),
  KEY idx_rpm_memoria_huella (huella),
  CONSTRAINT fk_rpm_memoria_analisis FOREIGN KEY (analisis_id) REFERENCES rpm_analisis(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Normalización y datos estructurales de roles.
UPDATE roles SET nombre = 'USUARIO_EMPRESA' WHERE UPPER(nombre) = 'USUARIO' AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM roles WHERE UPPER(nombre) = 'USUARIO_EMPRESA') x);
INSERT INTO roles (nombre, descripcion, activo, permisos) SELECT 'ADMINISTRADOR','Gobierno de plataforma, usuarios, empresas, catálogo y configuración',1,'{"dashboard":true,"usuarios":true,"roles":true,"empresas":true,"reportes":true,"configuracion":true}' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre)=UPPER('ADMINISTRADOR'));
UPDATE roles SET descripcion='Gobierno de plataforma, usuarios, empresas, catálogo y configuración', activo=1, permisos='{"dashboard":true,"usuarios":true,"roles":true,"empresas":true,"reportes":true,"configuracion":true}' WHERE UPPER(nombre)=UPPER('ADMINISTRADOR');
INSERT INTO roles (nombre, descripcion, activo, permisos) SELECT 'IMPLEMENTADOR','Construcción de la SoA, riesgos, controles y evidencias',1,'{"soa":true,"riesgos":true,"evidencias":true,"rpm":true}' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre)=UPPER('IMPLEMENTADOR'));
UPDATE roles SET descripcion='Construcción de la SoA, riesgos, controles y evidencias', activo=1, permisos='{"soa":true,"riesgos":true,"evidencias":true,"rpm":true}' WHERE UPPER(nombre)=UPPER('IMPLEMENTADOR');
INSERT INTO roles (nombre, descripcion, activo, permisos) SELECT 'AUDITOR','Validación de evidencias, hallazgos, firmas y resultados RPM',1,'{"auditoria":true,"evidencias":true,"rpm":true}' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre)=UPPER('AUDITOR'));
UPDATE roles SET descripcion='Validación de evidencias, hallazgos, firmas y resultados RPM', activo=1, permisos='{"auditoria":true,"evidencias":true,"rpm":true}' WHERE UPPER(nombre)=UPPER('AUDITOR');
INSERT INTO roles (nombre, descripcion, activo, permisos) SELECT 'CAPACITADOR','Gestión de formación, participantes, evaluaciones y recomendaciones RPM',1,'{"capacitaciones":true,"rpm":true}' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre)=UPPER('CAPACITADOR'));
UPDATE roles SET descripcion='Gestión de formación, participantes, evaluaciones y recomendaciones RPM', activo=1, permisos='{"capacitaciones":true,"rpm":true}' WHERE UPPER(nombre)=UPPER('CAPACITADOR');
INSERT INTO roles (nombre, descripcion, activo, permisos) SELECT 'USUARIO_EMPRESA','Portal ejecutivo de progreso, decisiones, riesgos y reportes',1,'{"portalEmpresa":true,"rpm":true,"reportes":true}' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre)=UPPER('USUARIO_EMPRESA'));
UPDATE roles SET descripcion='Portal ejecutivo de progreso, decisiones, riesgos y reportes', activo=1, permisos='{"portalEmpresa":true,"rpm":true,"reportes":true}' WHERE UPPER(nombre)=UPPER('USUARIO_EMPRESA');

-- Catálogo base de 93 controles: títulos y descripciones de trabajo para la aplicación.
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.1','Organizacional','Gobierno de políticas','establecer y mantener políticas de seguridad de la información','¿La organización ha definido, implementado, documentado y revisado medidas para establecer y mantener políticas de seguridad de la información?','gobierno,politicas','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.2','Organizacional','Roles de seguridad','asignar responsabilidades y autoridades de seguridad','¿La organización ha definido, implementado, documentado y revisado medidas para asignar responsabilidades y autoridades de seguridad?','gobierno,personas','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.3','Organizacional','Separación de funciones','separar tareas incompatibles y reducir conflictos o fraude','¿La organización ha definido, implementado, documentado y revisado medidas para separar tareas incompatibles y reducir conflictos o fraude?','gobierno,fraude','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.4','Organizacional','Responsabilidades de dirección','asegurar que la dirección exija el cumplimiento de las reglas de seguridad','¿La organización ha definido, implementado, documentado y revisado medidas para asegurar que la dirección exija el cumplimiento de las reglas de seguridad?','gobierno,personas','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.5','Organizacional','Relación con autoridades','mantener contactos apropiados con autoridades competentes','¿La organización ha definido, implementado, documentado y revisado medidas para mantener contactos apropiados con autoridades competentes?','legal,incidentes','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.6','Organizacional','Relación con comunidades especializadas','participar en redes y grupos de interés de seguridad','¿La organización ha definido, implementado, documentado y revisado medidas para participar en redes y grupos de interés de seguridad?','amenazas,gobierno','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.7','Organizacional','Inteligencia de amenazas','recopilar y analizar información sobre amenazas relevantes','¿La organización ha definido, implementado, documentado y revisado medidas para recopilar y analizar información sobre amenazas relevantes?','amenazas,monitoreo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.8','Organizacional','Seguridad en proyectos','integrar requisitos de seguridad en la gestión de proyectos','¿La organización ha definido, implementado, documentado y revisado medidas para integrar requisitos de seguridad en la gestión de proyectos?','proyectos,desarrollo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.9','Organizacional','Inventario de información y activos','identificar propietarios, ubicación y criticidad de activos','¿La organización ha definido, implementado, documentado y revisado medidas para identificar propietarios, ubicación y criticidad de activos?','activos,datos_sensibles','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.10','Organizacional','Uso aceptable de activos','definir reglas de uso y manejo de información y recursos','¿La organización ha definido, implementado, documentado y revisado medidas para definir reglas de uso y manejo de información y recursos?','activos,personas','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.11','Organizacional','Devolución de activos','recuperar activos cuando cambian o terminan relaciones laborales o contractuales','¿La organización ha definido, implementado, documentado y revisado medidas para recuperar activos cuando cambian o terminan relaciones laborales o contractuales?','activos,personas','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.12','Organizacional','Clasificación de información','clasificar información según sensibilidad, valor y requisitos','¿La organización ha definido, implementado, documentado y revisado medidas para clasificar información según sensibilidad, valor y requisitos?','datos_sensibles,privacidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.13','Organizacional','Marcado de información','aplicar etiquetas coherentes con la clasificación de información','¿La organización ha definido, implementado, documentado y revisado medidas para aplicar etiquetas coherentes con la clasificación de información?','datos_sensibles,privacidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.14','Organizacional','Transferencia de información','proteger intercambios internos y externos de información','¿La organización ha definido, implementado, documentado y revisado medidas para proteger intercambios internos y externos de información?','datos_sensibles,terceros,criptografia','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.15','Organizacional','Control de acceso','establecer reglas de acceso físico y lógico según necesidad y riesgo','¿La organización ha definido, implementado, documentado y revisado medidas para establecer reglas de acceso físico y lógico según necesidad y riesgo?','acceso,identidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.16','Organizacional','Gestión de identidades','administrar el ciclo de vida de identidades de usuarios y servicios','¿La organización ha definido, implementado, documentado y revisado medidas para administrar el ciclo de vida de identidades de usuarios y servicios?','identidad,acceso','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.17','Organizacional','Información de autenticación','proteger contraseñas, llaves, tokens y otros secretos de autenticación','¿La organización ha definido, implementado, documentado y revisado medidas para proteger contraseñas, llaves, tokens y otros secretos de autenticación?','identidad,acceso,criptografia','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.18','Organizacional','Derechos de acceso','aprobar, revisar y retirar permisos de acceso oportunamente','¿La organización ha definido, implementado, documentado y revisado medidas para aprobar, revisar y retirar permisos de acceso oportunamente?','identidad,acceso','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.19','Organizacional','Seguridad con proveedores','gestionar riesgos de seguridad asociados con proveedores y terceros','¿La organización ha definido, implementado, documentado y revisado medidas para gestionar riesgos de seguridad asociados con proveedores y terceros?','proveedores,terceros','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.20','Organizacional','Seguridad en acuerdos con proveedores','incorporar requisitos de seguridad en contratos y acuerdos','¿La organización ha definido, implementado, documentado y revisado medidas para incorporar requisitos de seguridad en contratos y acuerdos?','proveedores,legal','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.21','Organizacional','Cadena de suministro TIC','gestionar riesgos de productos y servicios dentro de la cadena de suministro tecnológica','¿La organización ha definido, implementado, documentado y revisado medidas para gestionar riesgos de productos y servicios dentro de la cadena de suministro tecnológica?','proveedores,nube,desarrollo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.22','Organizacional','Seguimiento de proveedores','revisar servicios, cambios y desempeño de seguridad de proveedores','¿La organización ha definido, implementado, documentado y revisado medidas para revisar servicios, cambios y desempeño de seguridad de proveedores?','proveedores,monitoreo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.23','Organizacional','Uso seguro de servicios en la nube','gobernar la adquisición, uso, cambio y salida de servicios cloud','¿La organización ha definido, implementado, documentado y revisado medidas para gobernar la adquisición, uso, cambio y salida de servicios cloud?','nube,proveedores','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.24','Organizacional','Preparación para incidentes','definir roles, procesos y capacidades para gestionar incidentes','¿La organización ha definido, implementado, documentado y revisado medidas para definir roles, procesos y capacidades para gestionar incidentes?','incidentes,continuidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.25','Organizacional','Evaluación de eventos de seguridad','analizar eventos y decidir si constituyen incidentes','¿La organización ha definido, implementado, documentado y revisado medidas para analizar eventos y decidir si constituyen incidentes?','incidentes,monitoreo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.26','Organizacional','Respuesta a incidentes','contener, resolver y comunicar incidentes de seguridad','¿La organización ha definido, implementado, documentado y revisado medidas para contener, resolver y comunicar incidentes de seguridad?','incidentes,continuidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.27','Organizacional','Aprendizaje de incidentes','usar lecciones aprendidas para evitar recurrencias','¿La organización ha definido, implementado, documentado y revisado medidas para usar lecciones aprendidas para evitar recurrencias?','incidentes,memoria','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.28','Organizacional','Recolección de evidencia','preservar evidencia con integridad y cadena de custodia','¿La organización ha definido, implementado, documentado y revisado medidas para preservar evidencia con integridad y cadena de custodia?','incidentes,evidencia,legal','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.29','Organizacional','Seguridad durante interrupciones','mantener protección de información durante situaciones adversas','¿La organización ha definido, implementado, documentado y revisado medidas para mantener protección de información durante situaciones adversas?','continuidad,servicio_critico','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.30','Organizacional','Preparación TIC para continuidad','asegurar recuperación y disponibilidad de tecnologías críticas','¿La organización ha definido, implementado, documentado y revisado medidas para asegurar recuperación y disponibilidad de tecnologías críticas?','continuidad,servicio_critico','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.31','Organizacional','Requisitos legales y contractuales','identificar y mantener obligaciones legales, regulatorias y contractuales','¿La organización ha definido, implementado, documentado y revisado medidas para identificar y mantener obligaciones legales, regulatorias y contractuales?','legal,privacidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.32','Organizacional','Derechos de propiedad intelectual','proteger software, contenidos, licencias y propiedad intelectual','¿La organización ha definido, implementado, documentado y revisado medidas para proteger software, contenidos, licencias y propiedad intelectual?','legal,propiedad_intelectual','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.33','Organizacional','Protección de registros','conservar registros íntegros, disponibles y protegidos durante su ciclo de vida','¿La organización ha definido, implementado, documentado y revisado medidas para conservar registros íntegros, disponibles y protegidos durante su ciclo de vida?','registros,legal','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.34','Organizacional','Privacidad y datos personales','cumplir requisitos de privacidad y protección de información personal','¿La organización ha definido, implementado, documentado y revisado medidas para cumplir requisitos de privacidad y protección de información personal?','privacidad,datos_sensibles,menores','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.35','Organizacional','Revisión independiente','evaluar de forma independiente el enfoque y la implementación de seguridad','¿La organización ha definido, implementado, documentado y revisado medidas para evaluar de forma independiente el enfoque y la implementación de seguridad?','auditoria,gobierno','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.36','Organizacional','Cumplimiento interno','verificar cumplimiento de políticas, reglas y estándares de seguridad','¿La organización ha definido, implementado, documentado y revisado medidas para verificar cumplimiento de políticas, reglas y estándares de seguridad?','auditoria,gobierno','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.5.37','Organizacional','Procedimientos operativos documentados','documentar procedimientos necesarios para operar de forma segura','¿La organización ha definido, implementado, documentado y revisado medidas para documentar procedimientos necesarios para operar de forma segura?','operacion,documentacion','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.6.1','Personas','Verificación de antecedentes','realizar verificaciones proporcionales antes y durante la vinculación','¿La organización ha definido, implementado, documentado y revisado medidas para realizar verificaciones proporcionales antes y durante la vinculación?','personas,acceso','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.6.2','Personas','Condiciones de vinculación','incluir responsabilidades de seguridad en términos laborales y contractuales','¿La organización ha definido, implementado, documentado y revisado medidas para incluir responsabilidades de seguridad en términos laborales y contractuales?','personas,legal','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.6.3','Personas','Concienciación y formación','formar periódicamente al personal según sus riesgos y responsabilidades','¿La organización ha definido, implementado, documentado y revisado medidas para formar periódicamente al personal según sus riesgos y responsabilidades?','personas,capacitacion','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.6.4','Personas','Proceso disciplinario','aplicar un proceso formal ante incumplimientos de seguridad','¿La organización ha definido, implementado, documentado y revisado medidas para aplicar un proceso formal ante incumplimientos de seguridad?','personas,gobierno','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.6.5','Personas','Responsabilidades tras cambios o retiro','mantener obligaciones y retirar accesos al cambiar o terminar una relación','¿La organización ha definido, implementado, documentado y revisado medidas para mantener obligaciones y retirar accesos al cambiar o terminar una relación?','personas,acceso','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.6.6','Personas','Acuerdos de confidencialidad','gestionar compromisos de confidencialidad aplicables','¿La organización ha definido, implementado, documentado y revisado medidas para gestionar compromisos de confidencialidad aplicables?','personas,privacidad,legal','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.6.7','Personas','Trabajo remoto seguro','proteger información, equipos y accesos usados fuera de las instalaciones','¿La organización ha definido, implementado, documentado y revisado medidas para proteger información, equipos y accesos usados fuera de las instalaciones?','trabajo_remoto,endpoint,nube','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.6.8','Personas','Reporte de eventos por el personal','facilitar el reporte oportuno de eventos y debilidades','¿La organización ha definido, implementado, documentado y revisado medidas para facilitar el reporte oportuno de eventos y debilidades?','personas,incidentes,capacitacion','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.1','Físico','Perímetros de seguridad física','delimitar y proteger áreas que contienen información o recursos críticos','¿La organización ha definido, implementado, documentado y revisado medidas para delimitar y proteger áreas que contienen información o recursos críticos?','fisico,infraestructura','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.2','Físico','Controles de entrada física','autorizar y registrar el acceso a instalaciones y áreas seguras','¿La organización ha definido, implementado, documentado y revisado medidas para autorizar y registrar el acceso a instalaciones y áreas seguras?','fisico,acceso','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.3','Físico','Protección de oficinas e instalaciones','diseñar y operar espacios físicos con medidas apropiadas','¿La organización ha definido, implementado, documentado y revisado medidas para diseñar y operar espacios físicos con medidas apropiadas?','fisico,infraestructura','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.4','Físico','Monitoreo de seguridad física','detectar y registrar accesos o actividades físicas no autorizadas','¿La organización ha definido, implementado, documentado y revisado medidas para detectar y registrar accesos o actividades físicas no autorizadas?','fisico,monitoreo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.5','Físico','Protección frente a amenazas ambientales','reducir riesgos por incendios, inundaciones, fallas y otros eventos','¿La organización ha definido, implementado, documentado y revisado medidas para reducir riesgos por incendios, inundaciones, fallas y otros eventos?','fisico,continuidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.6','Físico','Trabajo en áreas seguras','establecer reglas para actividades dentro de zonas protegidas','¿La organización ha definido, implementado, documentado y revisado medidas para establecer reglas para actividades dentro de zonas protegidas?','fisico,personas','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.7','Físico','Escritorio y pantalla despejados','evitar exposición de información en puestos y pantallas sin supervisión','¿La organización ha definido, implementado, documentado y revisado medidas para evitar exposición de información en puestos y pantallas sin supervisión?','fisico,personas,privacidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.8','Físico','Ubicación y protección de equipos','instalar equipos para reducir daño, acceso indebido e interrupciones','¿La organización ha definido, implementado, documentado y revisado medidas para instalar equipos para reducir daño, acceso indebido e interrupciones?','fisico,endpoint','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.9','Físico','Activos fuera de instalaciones','proteger activos cuando salen de las sedes de la organización','¿La organización ha definido, implementado, documentado y revisado medidas para proteger activos cuando salen de las sedes de la organización?','fisico,trabajo_remoto,endpoint','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.10','Físico','Medios de almacenamiento','gestionar adquisición, transporte, uso y disposición segura de medios','¿La organización ha definido, implementado, documentado y revisado medidas para gestionar adquisición, transporte, uso y disposición segura de medios?','datos_sensibles,activos','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.11','Físico','Servicios de soporte','proteger energía, climatización y otros servicios de infraestructura','¿La organización ha definido, implementado, documentado y revisado medidas para proteger energía, climatización y otros servicios de infraestructura?','continuidad,infraestructura','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.12','Físico','Seguridad del cableado','proteger cableado de energía y comunicaciones contra daño o interceptación','¿La organización ha definido, implementado, documentado y revisado medidas para proteger cableado de energía y comunicaciones contra daño o interceptación?','fisico,redes','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.13','Físico','Mantenimiento de equipos','mantener equipos de forma autorizada, registrada y segura','¿La organización ha definido, implementado, documentado y revisado medidas para mantener equipos de forma autorizada, registrada y segura?','mantenimiento,endpoint,ot','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.7.14','Físico','Eliminación o reutilización segura','borrar información y retirar licencias antes de disponer o reutilizar equipos','¿La organización ha definido, implementado, documentado y revisado medidas para borrar información y retirar licencias antes de disponer o reutilizar equipos?','datos_sensibles,endpoint','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.1','Tecnológico','Dispositivos de usuario final','configurar y proteger equipos de escritorio, portátiles y móviles','¿La organización ha definido, implementado, documentado y revisado medidas para configurar y proteger equipos de escritorio, portátiles y móviles?','endpoint,configuracion','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.2','Tecnológico','Accesos privilegiados','restringir, supervisar y revisar privilegios administrativos','¿La organización ha definido, implementado, documentado y revisado medidas para restringir, supervisar y revisar privilegios administrativos?','acceso,identidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.3','Tecnológico','Restricción de acceso a información','limitar acceso a información y funciones de acuerdo con permisos','¿La organización ha definido, implementado, documentado y revisado medidas para limitar acceso a información y funciones de acuerdo con permisos?','acceso,datos_sensibles','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.4','Tecnológico','Acceso al código fuente','proteger repositorios, bibliotecas y herramientas de código','¿La organización ha definido, implementado, documentado y revisado medidas para proteger repositorios, bibliotecas y herramientas de código?','desarrollo,propiedad_intelectual','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.5','Tecnológico','Autenticación segura','usar mecanismos de autenticación proporcionales al riesgo','¿La organización ha definido, implementado, documentado y revisado medidas para usar mecanismos de autenticación proporcionales al riesgo?','identidad,acceso','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.6','Tecnológico','Gestión de capacidad','vigilar y ajustar recursos para mantener desempeño y disponibilidad','¿La organización ha definido, implementado, documentado y revisado medidas para vigilar y ajustar recursos para mantener desempeño y disponibilidad?','continuidad,monitoreo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.7','Tecnológico','Protección contra software malicioso','prevenir, detectar y recuperar frente a malware','¿La organización ha definido, implementado, documentado y revisado medidas para prevenir, detectar y recuperar frente a malware?','malware,endpoint','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.8','Tecnológico','Gestión de vulnerabilidades técnicas','identificar, evaluar y tratar vulnerabilidades oportunamente','¿La organización ha definido, implementado, documentado y revisado medidas para identificar, evaluar y tratar vulnerabilidades oportunamente?','vulnerabilidades,parches','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.9','Tecnológico','Gestión de configuración','establecer líneas base y controlar configuraciones de hardware y software','¿La organización ha definido, implementado, documentado y revisado medidas para establecer líneas base y controlar configuraciones de hardware y software?','configuracion,nube,endpoint','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.10','Tecnológico','Eliminación de información','eliminar datos cuando ya no sean necesarios y exista autorización','¿La organización ha definido, implementado, documentado y revisado medidas para eliminar datos cuando ya no sean necesarios y exista autorización?','datos_sensibles,privacidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.11','Tecnológico','Enmascaramiento de datos','reducir exposición de información sensible mediante enmascaramiento','¿La organización ha definido, implementado, documentado y revisado medidas para reducir exposición de información sensible mediante enmascaramiento?','datos_sensibles,privacidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.12','Tecnológico','Prevención de fuga de datos','detectar y evitar divulgación o extracción no autorizada','¿La organización ha definido, implementado, documentado y revisado medidas para detectar y evitar divulgación o extracción no autorizada?','datos_sensibles,monitoreo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.13','Tecnológico','Copias de seguridad','realizar respaldos y comprobar periódicamente su restauración','¿La organización ha definido, implementado, documentado y revisado medidas para realizar respaldos y comprobar periódicamente su restauración?','backup,continuidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.14','Tecnológico','Redundancia de procesamiento','disponer redundancia suficiente para requisitos de disponibilidad','¿La organización ha definido, implementado, documentado y revisado medidas para disponer redundancia suficiente para requisitos de disponibilidad?','continuidad,servicio_critico','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.15','Tecnológico','Registro de eventos','generar, proteger y revisar logs relevantes','¿La organización ha definido, implementado, documentado y revisado medidas para generar, proteger y revisar logs relevantes?','logs,monitoreo','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.16','Tecnológico','Monitoreo de actividades','observar redes, sistemas y aplicaciones para detectar comportamientos anómalos','¿La organización ha definido, implementado, documentado y revisado medidas para observar redes, sistemas y aplicaciones para detectar comportamientos anómalos?','monitoreo,incidentes','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.17','Tecnológico','Sincronización de relojes','sincronizar relojes para preservar trazabilidad temporal','¿La organización ha definido, implementado, documentado y revisado medidas para sincronizar relojes para preservar trazabilidad temporal?','logs,infraestructura','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.18','Tecnológico','Herramientas con privilegios especiales','controlar utilidades capaces de evadir controles del sistema','¿La organización ha definido, implementado, documentado y revisado medidas para controlar utilidades capaces de evadir controles del sistema?','acceso,configuracion','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.19','Tecnológico','Instalación de software','autorizar y controlar software instalado en sistemas operativos','¿La organización ha definido, implementado, documentado y revisado medidas para autorizar y controlar software instalado en sistemas operativos?','configuracion,endpoint','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.20','Tecnológico','Seguridad de redes','proteger redes, dispositivos y tráfico frente a accesos o alteraciones','¿La organización ha definido, implementado, documentado y revisado medidas para proteger redes, dispositivos y tráfico frente a accesos o alteraciones?','redes,infraestructura','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.21','Tecnológico','Seguridad de servicios de red','definir y supervisar requisitos de seguridad de servicios de red','¿La organización ha definido, implementado, documentado y revisado medidas para definir y supervisar requisitos de seguridad de servicios de red?','redes,proveedores','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.22','Tecnológico','Segmentación de redes','separar grupos, servicios y sistemas según riesgo','¿La organización ha definido, implementado, documentado y revisado medidas para separar grupos, servicios y sistemas según riesgo?','redes,ot,nube','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.23','Tecnológico','Filtrado web','reducir acceso a recursos web maliciosos o no autorizados','¿La organización ha definido, implementado, documentado y revisado medidas para reducir acceso a recursos web maliciosos o no autorizados?','endpoint,malware','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.24','Tecnológico','Uso de criptografía','gobernar cifrado, llaves y certificados según riesgo','¿La organización ha definido, implementado, documentado y revisado medidas para gobernar cifrado, llaves y certificados según riesgo?','criptografia,datos_sensibles','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.25','Tecnológico','Ciclo de desarrollo seguro','integrar seguridad en todas las etapas del desarrollo','¿La organización ha definido, implementado, documentado y revisado medidas para integrar seguridad en todas las etapas del desarrollo?','desarrollo,seguridad_aplicaciones','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.26','Tecnológico','Requisitos de seguridad de aplicaciones','definir y aprobar requisitos de seguridad antes de construir o adquirir aplicaciones','¿La organización ha definido, implementado, documentado y revisado medidas para definir y aprobar requisitos de seguridad antes de construir o adquirir aplicaciones?','desarrollo,seguridad_aplicaciones','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.27','Tecnológico','Arquitectura e ingeniería segura','aplicar principios de diseño y arquitectura segura','¿La organización ha definido, implementado, documentado y revisado medidas para aplicar principios de diseño y arquitectura segura?','desarrollo,seguridad_aplicaciones','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.28','Tecnológico','Codificación segura','aplicar reglas y revisiones para reducir defectos de seguridad en código','¿La organización ha definido, implementado, documentado y revisado medidas para aplicar reglas y revisiones para reducir defectos de seguridad en código?','desarrollo,seguridad_aplicaciones','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.29','Tecnológico','Pruebas de seguridad','realizar pruebas de seguridad durante desarrollo y aceptación','¿La organización ha definido, implementado, documentado y revisado medidas para realizar pruebas de seguridad durante desarrollo y aceptación?','desarrollo,vulnerabilidades','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.30','Tecnológico','Desarrollo subcontratado','gobernar y revisar seguridad en desarrollo realizado por terceros','¿La organización ha definido, implementado, documentado y revisado medidas para gobernar y revisar seguridad en desarrollo realizado por terceros?','desarrollo,proveedores','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.31','Tecnológico','Separación de ambientes','separar y proteger entornos de desarrollo, pruebas y producción','¿La organización ha definido, implementado, documentado y revisado medidas para separar y proteger entornos de desarrollo, pruebas y producción?','desarrollo,configuracion','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.32','Tecnológico','Gestión de cambios','evaluar, autorizar, probar y registrar cambios de sistemas','¿La organización ha definido, implementado, documentado y revisado medidas para evaluar, autorizar, probar y registrar cambios de sistemas?','cambios,configuracion','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.33','Tecnológico','Información de prueba','proteger datos usados en pruebas y evitar exposición de información real','¿La organización ha definido, implementado, documentado y revisado medidas para proteger datos usados en pruebas y evitar exposición de información real?','desarrollo,datos_sensibles,privacidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;
INSERT INTO catalogo_controles (codigo, dominio, titulo, descripcion, pregunta_evaluacion, etiquetas, version_norma, activo) VALUES ('A.8.34','Tecnológico','Protección durante auditorías técnicas','planificar pruebas de auditoría para evitar impactos en sistemas operativos','¿La organización ha definido, implementado, documentado y revisado medidas para planificar pruebas de auditoría para evitar impactos en sistemas operativos?','auditoria,continuidad','ISO/IEC 27001:2022',1) ON DUPLICATE KEY UPDATE dominio=VALUES(dominio), titulo=VALUES(titulo), descripcion=VALUES(descripcion), pregunta_evaluacion=VALUES(pregunta_evaluacion), etiquetas=VALUES(etiquetas), version_norma=VALUES(version_norma), activo=1;

-- Crea perfiles básicos y registros SoA para servicios existentes; la recomendación contextual se recalcula al iniciar el backend.
INSERT INTO perfiles_organizacionales (empresa_id, sector_id, actualizado_en)
SELECT s.empresa_id, MAX(s.sector_id), CURRENT_TIMESTAMP(6)
FROM servicios s
LEFT JOIN perfiles_organizacionales p ON p.empresa_id = s.empresa_id
WHERE p.id IS NULL
GROUP BY s.empresa_id;
INSERT INTO soa_controles (servicio_id, control_id, aplicabilidad, estado_implementacion, porcentaje_implementacion, puntaje_relevancia, creado_en, actualizado_en, version)
SELECT s.id, c.id, 'PENDIENTE', 'NO_INICIADO', 0, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 0
FROM servicios s
CROSS JOIN catalogo_controles c
LEFT JOIN soa_controles sc ON sc.servicio_id = s.id AND sc.control_id = c.id
WHERE c.activo = 1 AND sc.id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
DROP PROCEDURE IF EXISTS add_fk_if_missing;

-- Fin de migración. El backend usa Hibernate update como respaldo, pero esta migración permite instalaciones controladas.
