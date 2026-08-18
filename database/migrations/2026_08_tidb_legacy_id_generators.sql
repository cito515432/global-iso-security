-- Compatibilidad de IDs para tablas heredadas en TiDB Cloud.
-- Evita reconstruir 12 tablas antiguas que fueron importadas sin AUTO_INCREMENT.
-- Hibernate usa esta tabla con GenerationType.TABLE para asignar IDs nuevos.
USE globalisosecurity;

CREATE TABLE IF NOT EXISTS legacy_id_generators (
  entity_name VARCHAR(64) NOT NULL,
  next_val BIGINT NOT NULL,
  PRIMARY KEY (entity_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- No se insertan filas manualmente. Hibernate crea un segmento por entidad
-- al primer INSERT, iniciando desde 1,000,000 para no colisionar con IDs legados.
SELECT 'legacy_id_generators_ready' AS estado;
