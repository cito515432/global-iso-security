-- Integración del componente Machine Learning al análisis RPM.
-- Compatible con TiDB Cloud. Ejecutar con SPRING_JPA_HIBERNATE_DDL_AUTO=none.
USE globalisosecurity;

ALTER TABLE rpm_analisis ADD COLUMN IF NOT EXISTS prioridad_ml VARCHAR(20) NULL;
ALTER TABLE rpm_analisis ADD COLUMN IF NOT EXISTS confianza_ml DOUBLE NULL;
ALTER TABLE rpm_analisis ADD COLUMN IF NOT EXISTS probabilidades_ml TEXT NULL;
ALTER TABLE rpm_analisis ADD COLUMN IF NOT EXISTS version_modelo_ml VARCHAR(80) NULL;
ALTER TABLE rpm_analisis ADD COLUMN IF NOT EXISTS ml_estado VARCHAR(30) NULL DEFAULT 'PENDIENTE';
ALTER TABLE rpm_analisis ADD COLUMN IF NOT EXISTS ml_generado_en DATETIME NULL;

UPDATE rpm_analisis SET ml_estado='PENDIENTE' WHERE ml_estado IS NULL;

SELECT COUNT(*) AS analisis_rpm_existentes FROM rpm_analisis;
SELECT COUNT(*) AS predicciones_ml_existentes FROM rpm_analisis WHERE prioridad_ml IS NOT NULL;
SELECT 'rpm_ml_columns_ready' AS estado;
