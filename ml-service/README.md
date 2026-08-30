# RPM ML Service

Microservicio FastAPI que carga `modelo_rpm_rf_humano_v1.joblib` y expone predicciones de prioridad como apoyo al motor RPM determinista.

- Modelo: Random Forest experimental.
- Fuente de entrenamiento/validación: 80 escenarios sintéticos controlados con validación humana independiente.
- No reemplaza la decisión del responsable del SGSI.
- Endpoints: `/health`, `/metadata`, `/predict`, `/predict/batch`.
- Puede protegerse con `ML_API_KEY`; el backend debe usar el mismo valor en `RPM_ML_API_KEY`.
