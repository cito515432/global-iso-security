# Integración RPM + Machine Learning

## Propósito

La aplicación conserva el motor `RPM-DETERMINISTA-1.0` como núcleo explicable y añade una segunda estimación de prioridad mediante un modelo Random Forest experimental (`RPM-ML-RF-HUMANO-V1`). La estimación ML es un apoyo a la decisión y nunca sustituye la validación humana.

El modelo fue entrenado a partir de una muestra de 80 escenarios sintéticos controlados con validación humana independiente. En validación cruzada agrupada por organización, Random Forest obtuvo aproximadamente 72,5 % de exactitud y F1 macro de 68,3 %. Estos resultados son experimentales y no representan certificación, exactitud sobre empresas reales ni consenso experto.

## Arquitectura

```text
SoA + riesgos + evidencias + hallazgos + contexto
                 |
                 v
        RPM determinista (Java)
        puntaje + señales + acciones
                 |
                 +------------------+
                 |                  |
                 v                  v
        Microservicio ML       Memoria RPM
        FastAPI / RF           casos validados
        prioridad + prob.
                 |
                 v
        Comparación RPM vs ML
                 |
                 v
          Validación humana
                 |
                 v
            Decisión final
```

## Variables enviadas al modelo

- sector
- tamaño organizacional
- dominio del control
- indicador de componente humano
- aplicabilidad
- estado de implementación
- porcentaje de implementación
- relevancia contextual
- fecha objetivo vencida
- probabilidad, impacto y nivel inherente del riesgo
- categoría del riesgo
- evidencias totales, pendientes, rechazadas y vencidas
- hallazgos abiertos, recurrentes y severidad máxima

No se envían al modelo el puntaje RPM, la prioridad RPM ni las señales RPM como variables de entrada. Esto evita fuga directa de la respuesta determinista.

## Persistencia

La migración `database/migrations/2026_08_rpm_ml_hibrido_tidb.sql` agrega a `rpm_analisis`:

- `prioridad_ml`
- `confianza_ml`
- `probabilidades_ml`
- `version_modelo_ml`
- `ml_estado`
- `ml_generado_en`

La memoria RPM también registra la prioridad y versión ML disponibles en el momento de memorizar el caso.

## Endpoints nuevos del backend

- `GET /api/rpm/ml/estado`
- `POST /api/rpm/ml/predecir/servicio/{servicioId}`
- `POST /api/rpm/ml/predecir/soa/{soaId}`

## Endpoints del microservicio ML

- `GET /health`
- `GET /metadata`
- `POST /predict`
- `POST /predict/batch`

## Despliegue en Render

### 1. Migrar TiDB

Mantener:

```text
SPRING_JPA_HIBERNATE_DDL_AUTO=none
```

Ejecutar en TiDB Cloud SQL Editor:

```text
database/migrations/2026_08_rpm_ml_hibrido_tidb.sql
```

### 2. Crear servicio ML

En Render crear un nuevo Web Service desde el mismo repositorio y rama `rpm-integral`:

- Name: `globaliso-ml-cito515432`
- Runtime: Docker
- Dockerfile Path: `./ml-service/Dockerfile.render`
- Docker Context: `./ml-service`
- Plan: Free
- Health Check: `/health`

Variables:

```text
PORT=10000
RPM_ML_MODEL_VERSION=RPM-ML-RF-HUMANO-V1
RPM_ML_HUMAN_REVIEW_THRESHOLD=0.70
ML_API_KEY=<clave privada aleatoria>
```

Abrir después:

```text
https://globaliso-ml-cito515432.onrender.com/health
```

Debe responder `status: ok`.

### 3. Configurar backend

En el servicio backend agregar:

```text
RPM_ML_ENABLED=true
RPM_ML_URL=https://globaliso-ml-cito515432.onrender.com
RPM_ML_API_KEY=<misma clave usada en ML_API_KEY>
RPM_ML_TIMEOUT_SECONDS=120
SPRING_JPA_HIBERNATE_DDL_AUTO=none
```

Desplegar el nuevo commit del backend.

### 4. Desplegar frontend

Desplegar el mismo commit en el frontend. No se requieren variables nuevas del frontend.

## Prueba funcional

1. Entrar como implementador o auditor.
2. Abrir RPM.
3. Pulsar `Analizar RPM + ML`.
4. El frontend ejecuta primero RPM determinista y después la predicción ML por lote.
5. Cada tarjeta debe mostrar:
   - prioridad y puntaje RPM;
   - prioridad ML;
   - probabilidad estimada;
   - versión del modelo;
   - advertencia si existe discrepancia o confianza baja.
6. Aprobar, modificar o rechazar la decisión como antes.
7. Registrar el resultado en memoria.

## Tolerancia a fallos

Si el microservicio ML está dormido o no responde, el backend no invalida el motor determinista. La interfaz informa que la estimación ML quedó pendiente y puede reintentarse. Esto es intencional: Machine Learning es un componente de apoyo, no un punto único de falla.

## Limitaciones metodológicas

- El modelo actual se basa en 80 validaciones humanas sobre escenarios sintéticos controlados.
- La probabilidad de Random Forest se presenta como confianza estimada experimental; no es una probabilidad calibrada de certeza profesional.
- La salida ML no debe ejecutar automáticamente acciones.
- El sistema debe conservar la validación humana y continuar recolectando memoria para futuros reentrenamientos.
