# Global ISO Security — despliegue actual en Render + TiDB Cloud

La arquitectura desplegada para el proyecto académico es:

```text
Navegador
   |
   v
Frontend Nginx — Render
   |
   v
Backend Spring Boot — Render
   |                 \
   v                  v
TiDB Cloud        RPM ML FastAPI — Render
                      |
                      v
              Random Forest V1
```

## Servicios

- `globaliso-frontend-cito515432`: frontend Nginx.
- `globaliso-backend-cito515432`: backend Spring Boot.
- `globaliso-ml-cito515432`: microservicio ML experimental.
- Base de datos: TiDB Cloud externa, compatible con protocolo MySQL.

## Reglas críticas de seguridad

Mantener siempre en el backend:

```text
SPRING_JPA_HIBERNATE_DDL_AUTO=none
SPRING_JPA_SHOW_SQL=false
SEED_DEMO_DATA=false
```

Los cambios de esquema se aplican mediante migraciones SQL controladas en `database/migrations/`. No publique contraseñas de administradores, credenciales de TiDB, JWT ni claves ML en el repositorio, HTML, capturas o documentación.

## Variables del backend

```text
SPRING_DATASOURCE_URL=<JDBC TiDB con TLS>
SPRING_DATASOURCE_USERNAME=<usuario TiDB>
SPRING_DATASOURCE_PASSWORD=<secreto TiDB>
JWT_SECRET=<secreto aleatorio de 32+ caracteres>
JWT_EXPIRATION=3600000
SPRING_JPA_HIBERNATE_DDL_AUTO=none
SPRING_JPA_SHOW_SQL=false
SEED_ENABLED=true
SEED_DEMO_DATA=false
CORS_ALLOWED_ORIGINS=https://globaliso-frontend-cito515432.onrender.com
RPM_ML_ENABLED=true
RPM_ML_URL=https://globaliso-ml-cito515432.onrender.com
RPM_ML_API_KEY=<clave aleatoria de 32+ caracteres compartida con ML_API_KEY>
RPM_ML_TIMEOUT_SECONDS=120
```

En producción, `SEED_ENABLED=false` evita ejecutar la reconciliación masiva de servicios y SoA durante cada arranque. La base productiva auditada ya contiene roles, sectores, catálogo, perfiles y cobertura SoA completa; además, la creación de un servicio inicializa su SoA mediante el flujo normal de negocio. `SEED_ENABLED=true` queda reservado para desarrollo, CI, bootstrap explícito o recuperación controlada.

`SEED_DEMO_DATA=false` debe mantenerse siempre en producción. Una cuenta administrativa solo se crea si se proporcionan explícitamente `SEED_ADMIN_EMAIL` y `SEED_ADMIN_PASSWORD`. No existe contraseña administrativa por defecto.

## Variables del servicio ML

```text
PORT=10000
ML_API_KEY=<misma clave privada de 32+ caracteres que RPM_ML_API_KEY>
RPM_ML_MODEL_VERSION=RPM-ML-RF-HUMANO-V1
RPM_ML_HUMAN_REVIEW_THRESHOLD=0.70
```

El servicio ML falla de forma segura al arrancar si `ML_API_KEY` falta o tiene menos de 32 caracteres. `/health` permanece público para Render; `/metadata`, `/predict` y `/predict/batch` requieren la clave privada.

## Variables del frontend

```text
PORT=10000
BACKEND_URL=https://globaliso-backend-cito515432.onrender.com
```

El frontend publica cabeceras de seguridad (CSP, anti-clickjacking, no-sniff, HSTS y políticas de permisos) y el navegador consume el backend a través del proxy `/api`.

## Almacenamiento de evidencias en el plan Free

El backend usa `EVIDENCE_STORAGE_PATH` y valida las rutas fuera del repositorio, pero el servicio Render actual no tiene Persistent Disk porque los discos no están disponibles en el plan Free. Por tanto, los archivos subidos a `./storage/evidencias` son efímeros y pueden perderse después de un redeploy o reinicio.

La base de datos conserva la referencia y el hash, pero no reemplaza el archivo físico. Para preservar evidencias de forma permanente se necesitaría almacenamiento externo compatible o un plan con disco persistente; esto queda documentado como limitación operativa y no se habilita dentro del despliegue gratuito.

## Orden recomendado de despliegue

1. Hacer backup de TiDB.
2. Confirmar/rotar `JWT_SECRET`, `ML_API_KEY` y `RPM_ML_API_KEY`.
3. Mantener el mismo valor en `ML_API_KEY` y `RPM_ML_API_KEY`.
4. Desplegar `globaliso-ml-cito515432` y verificar `/health`.
5. Desplegar backend con el mismo commit y verificar `/health`.
6. Desplegar frontend con el mismo commit.
7. Probar login, aislamiento por empresa, carga/descarga de evidencia y `Analizar RPM + ML`.
8. Confirmar que los logs de auditoría solo son consultables por ADMINISTRADOR y que no existe DELETE/POST público para logs.

## Tolerancia a fallos

El motor RPM determinista no depende del ML para funcionar. Si el servicio ML está dormido o temporalmente no responde, la aplicación conserva el análisis determinista y la interfaz muestra la estimación ML como pendiente.

Consulta `SECURITY.md`, `docs/SECURITY_HARDENING_2026.md` y `docs/INTEGRACION_ML_RPM.md` para el detalle.
