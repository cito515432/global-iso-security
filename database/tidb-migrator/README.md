# Ejecutor controlado de migraciones TiDB

Este módulo reutiliza las migraciones Flyway del backend desde `backend/src/main/resources/db/migration`; no mantiene otra copia SQL. Está destinado a una ejecución puntual desde un entorno controlado con acceso a TiDB. No se conecta desde Android y no forma parte del proceso normal de arranque de Render.

Las dependencias Java usan Flyway Core/MySQL `11.9.0` y el complemento comunitario TiDB `10.26.0`; el complemento tiene versionado independiente y declara esa compatibilidad.

## Modos

- `info` (predeterminado): conexión de solo lectura; muestra historial y migraciones pendientes.
- `validate`: valida checksums de migraciones aplicadas.
- `migrate`: aplica hasta la versión 2 y exige confirmaciones explícitas, TLS con verificación de identidad y esquema esperado.

La ejecución aborta si detecta esquemas parciales o tablas que ya existen mientras su migración correspondiente aparece pendiente. La versión 1 agrega MFA; la versión 2 agrega limitación persistente de intentos y tokens de restablecimiento de contraseña. No ejecuta `clean` ni modifica tablas de negocio, servicios, controles o relaciones SoA.

## Configuración segura

Proporciona las variables desde el gestor de secretos del entorno, nunca desde un `.env` versionado ni en argumentos de línea de comandos:

- `TIDB_JDBC_URL`: URL JDBC del clúster TiDB Cloud con `sslMode=VERIFY_IDENTITY`.
- `TIDB_USERNAME` y `TIDB_PASSWORD`.
- `TIDB_ALLOWED_HOST_SUFFIX`: opcional; predeterminado `tidbcloud.com`, úsalo solo si tu endpoint TiDB válido tiene otro sufijo.
- Para `migrate` con V1 pendiente: `TIDB_MIGRATION_CONFIRM=APPLY_MFA_V1`.
- Para `migrate` con V2 pendiente: `TIDB_SECURITY_MIGRATION_CONFIRM=APPLY_SECURITY_V2`.
- Para cualquier escritura: `TIDB_BACKUP_VERIFIED=true`, `TIDB_BACKUP_REFERENCE=<identificador>` y `TIDB_EXPECTED_DATABASE=globalisosecurity`.

`TIDB_BACKUP_VERIFIED` es una barrera de confirmación operativa, no una verificación automática del proveedor. Antes de habilitarla, valida en TiDB Cloud que el backup existe y que es restaurable.

## Ejecución

Desde la raíz del repositorio:

```bash
mvn -f database/tidb-migrator/pom.xml -DskipTests package
java -jar database/tidb-migrator/target/globaliso-tidb-migrator-1.0.0.jar info
java -jar database/tidb-migrator/target/globaliso-tidb-migrator-1.0.0.jar validate
```

El modo de escritura requiere preparar los secretos en el entorno protegido y solo se debe ejecutar después de verificar el backup, revisar el estado `info`, validar el endpoint/BD de destino y autorizar la ventana de cambio:

```bash
java -jar database/tidb-migrator/target/globaliso-tidb-migrator-1.0.0.jar migrate
```

No ejecutes simultáneamente `migrate` desde este módulo y desde el arranque del backend. El backend mantiene `DB_MIGRATIONS_ENABLED=false`; el migrador es el único camino de escritura controlado para esta tarea.
