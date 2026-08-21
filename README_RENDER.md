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

## Regla crítica para TiDB

Mantener siempre en el backend:

```text
SPRING_JPA_HIBERNATE_DDL_AUTO=none
```

Los cambios de esquema se aplican mediante migraciones SQL controladas en `database/migrations/`.

## Variables del backend

Además de las credenciales de TiDB y JWT:

```text
RPM_ML_ENABLED=true
RPM_ML_URL=https://globaliso-ml-cito515432.onrender.com
RPM_ML_API_KEY=<clave privada compartida con ML_API_KEY>
RPM_ML_TIMEOUT_SECONDS=120
```

## Variables del servicio ML

```text
PORT=10000
ML_API_KEY=<misma clave privada que RPM_ML_API_KEY>
RPM_ML_MODEL_VERSION=RPM-ML-RF-HUMANO-V1
RPM_ML_HUMAN_REVIEW_THRESHOLD=0.70
```

## Variables del frontend

```text
PORT=10000
BACKEND_URL=https://globaliso-backend-cito515432.onrender.com
```

## Orden recomendado de despliegue

1. Ejecutar las migraciones TiDB necesarias.
2. Desplegar `globaliso-ml-cito515432` y verificar `/health`.
3. Desplegar backend con el mismo commit y verificar `/health`.
4. Desplegar frontend con el mismo commit.
5. Entrar como implementador/auditor y ejecutar `Analizar RPM + ML`.

## Tolerancia a fallos

El motor RPM determinista no depende del ML para funcionar. Si el servicio ML está dormido o temporalmente no responde, la aplicación conserva el análisis determinista y la interfaz muestra la estimación ML como pendiente.

Consulta `docs/INTEGRACION_ML_RPM.md` para el procedimiento detallado.
