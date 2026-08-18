# Compatibilidad de IDs heredados con TiDB Cloud

La base histórica fue importada desde MySQL con varias columnas `id` sin el atributo `AUTO_INCREMENT`. Las entidades Java originales usaban `GenerationType.IDENTITY`, por lo que cualquier INSERT nuevo sobre esas tablas fallaba con `Field 'id' doesn't have a default value`.

Para evitar reconstruir tablas con relaciones existentes, las 12 entidades heredadas usan ahora `GenerationType.TABLE` y una tabla auxiliar `legacy_id_generators`.

Entidades adaptadas:
- Capacitacion
- Checklist
- ConstanciaCapacitacion
- Empresa
- Evaluacion
- Firma
- ItemChecklist
- LogAuditoria
- Rol
- Sector
- Servicio
- Usuario

## Paso previo en TiDB

Ejecutar una sola vez:

`database/migrations/2026_08_tidb_legacy_id_generators.sql`

La tabla auxiliar no requiere semillas manuales. Hibernate crea un segmento por entidad cuando necesita el primer ID. Los nuevos identificadores parten de 1,000,000 para no colisionar con los IDs históricos.

## Render

Mantener:
- `SPRING_JPA_HIBERNATE_DDL_AUTO=none`
- `SEED_ENABLED=true` después de aplicar la migración auxiliar
- `SEED_DEMO_DATA=true` únicamente mientras se prueban los roles demo
