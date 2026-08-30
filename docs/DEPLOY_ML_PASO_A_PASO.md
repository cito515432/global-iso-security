# Despliegue paso a paso — RPM híbrido + ML

Este procedimiento asume el estado actual del proyecto:

- Rama de trabajo: `rpm-integral`.
- Backend: `globaliso-backend-cito515432` en Render.
- Frontend: `globaliso-frontend-cito515432` en Render.
- Base de datos: TiDB Cloud.
- `SPRING_JPA_HIBERNATE_DDL_AUTO=none`.

## 1. Copiar el parche

Descomprima el ZIP de integración ML y copie su contenido sobre la raíz de su clon local `global-iso-security`, aceptando reemplazar archivos existentes.

## 2. Migrar TiDB

Haga backup de TiDB. Después ejecute en SQL Editor:

```text
database/migrations/2026_08_rpm_ml_hibrido_tidb.sql
```

Verifique:

```sql
SHOW COLUMNS FROM rpm_analisis LIKE 'prioridad_ml';
SHOW COLUMNS FROM rpm_analisis LIKE 'confianza_ml';
```

## 3. Subir a GitHub

```bash
git switch rpm-integral
git status
git add -A
git commit -m "feat: integra RPM hibrido con Machine Learning"
git push origin rpm-integral
git log -1 --oneline
```

Copie el nuevo SHA.

## 4. Crear el microservicio ML en Render

Render → **New +** → **Web Service** → repositorio `cito515432/global-iso-security`.

Configuración:

```text
Name: globaliso-ml-cito515432
Branch: rpm-integral
Runtime: Docker
Dockerfile Path: ./ml-service/Dockerfile.render
Docker Context: ./ml-service
Plan: Free
Health Check Path: /health
```

Variables:

```text
PORT=10000
RPM_ML_MODEL_VERSION=RPM-ML-RF-HUMANO-V1
RPM_ML_HUMAN_REVIEW_THRESHOLD=0.70
ML_API_KEY=<GENERE_UN_SECRETO_PRIVADO>
```

No reutilice la contraseña del administrador ni el JWT como API key.

Cuando quede Live, pruebe:

```text
https://globaliso-ml-cito515432.onrender.com/health
https://globaliso-ml-cito515432.onrender.com/metadata
```

## 5. Conectar el backend al ML

Render → backend → Environment. Mantenga las variables actuales de TiDB/JWT y agregue:

```text
RPM_ML_ENABLED=true
RPM_ML_URL=https://globaliso-ml-cito515432.onrender.com
RPM_ML_API_KEY=<EL_MISMO_VALOR_DE_ML_API_KEY>
RPM_ML_TIMEOUT_SECONDS=120
SPRING_JPA_HIBERNATE_DDL_AUTO=none
```

Despliegue el nuevo SHA específico.

Pruebe:

```text
https://globaliso-backend-cito515432.onrender.com/health
```

## 6. Desplegar frontend

Render → frontend → Manual Deploy → Deploy a specific commit → pegue el mismo SHA.

No agregue `:10000` a la URL pública. Use:

```text
https://globaliso-frontend-cito515432.onrender.com
```

## 7. Prueba en la aplicación

Como Implementador o Auditor:

1. Abra la sección RPM.
2. Pulse `Analizar RPM + ML`.
3. Espere la primera vez: el servicio ML Free puede estar dormido.
4. Cada análisis debe mostrar:
   - prioridad RPM y puntaje /100;
   - estimación ML;
   - confianza estimada;
   - versión del modelo;
   - aviso si RPM y ML difieren.
5. La validación humana continúa disponible: Aprobar / Modificar / Rechazar.
6. Registre posteriormente el resultado en memoria RPM.

## 8. Qué hacer si ML falla

No se pierde el análisis RPM. La arquitectura está diseñada para degradación segura:

```text
ML no disponible -> RPM determinista sigue funcionando -> ML queda pendiente -> reintentar luego
```

Revise primero:

```text
https://globaliso-ml-cito515432.onrender.com/health
```

Después confirme que `RPM_ML_API_KEY` y `ML_API_KEY` son idénticas.

## 9. Importante para la tesis

La interfaz debe describir el ML como **experimental y de apoyo**. El modelo actual fue evaluado sobre 80 escenarios sintéticos controlados con validación humana independiente. La salida no sustituye al responsable del SGSI y no debe ejecutar acciones automáticamente.
