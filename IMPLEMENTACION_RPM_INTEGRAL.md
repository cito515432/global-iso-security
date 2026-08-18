# Implementación integral SoA + RPM

## 1. Propósito de esta versión

Esta versión convierte el prototipo inicial de Global ISO Security en una base funcional para el proyecto de grado. Se conservó la arquitectura Spring Boot/MySQL y se reemplazaron los flujos más simulados por componentes persistentes, relacionados entre sí y protegidos por roles.

La solución se organiza alrededor de este flujo:

```text
Empresa y contexto
        ↓
Riesgos y controles necesarios
        ↓
Declaración de Aplicabilidad
        ↓
Implementación y evidencias
        ↓
Auditoría y hallazgos
        ↓
Análisis RPM y decisiones humanas
        ↓
Acciones, formación y seguimiento
        ↓
Memoria y evaluación de resultados
```

## 2. Componentes incorporados

### 2.1 Catálogo y SoA

- Catálogo maestro de 93 controles.
- Registro SoA por servicio y empresa.
- Aplicabilidad `PENDIENTE`, `APLICABLE` o `NO_APLICABLE`.
- Justificación obligatoria para controles no aplicables.
- Estado de implementación, porcentaje, responsable, observaciones y fecha objetivo.
- Relevancia contextual calculada a partir del perfil organizacional.

### 2.2 Contexto de la organización

Se registra sector y variables operativas que afectan la relevancia de los controles. El sector no elimina controles; genera recomendaciones que deben ser revisadas por el implementador.

### 2.3 Gestión de riesgos

- Activo de información.
- Amenaza, vulnerabilidad y consecuencia.
- Probabilidad e impacto.
- Nivel inherente y residual.
- Tratamiento, responsable, estado y fecha de revisión.
- Relación muchos-a-muchos entre riesgos y controles.

### 2.4 Evidencias

- Carga multipart de archivos.
- Nombres interno y original.
- Tipo MIME y hash SHA-256.
- Descripción, clase de evidencia y fecha de vencimiento.
- Validación o rechazo por auditor.
- Descarga controlada por empresa.
- Almacenamiento persistente mediante volumen de Docker.

### 2.5 Auditoría

- Hallazgos relacionados con control y riesgo.
- Severidad, recurrencia, estado, fecha de detección y cierre.
- Firmas con control de acceso.
- Logs de acciones relevantes.

### 2.6 Motor RPM

El motor procesa el estado actual de cada control y combina:

- aplicabilidad y nivel de implementación;
- evidencias, vigencia y resultado de validación;
- riesgos asociados;
- hallazgos abiertos o recurrentes;
- fecha objetivo;
- contexto de la organización;
- casos históricos similares.

Produce:

- antígenos y señales de peligro;
- puntaje de 0 a 100;
- prioridad `BAJA`, `MEDIA`, `ALTA` o `CRITICA`;
- explicación de los factores;
- decisiones sugeridas;
- validación humana;
- memoria del caso y resultado.

### 2.7 Portal Empresa

La organización dispone de una vista propia con:

- avance por etapa;
- resumen de SoA;
- riesgos;
- decisiones RPM;
- formación;
- contexto;
- reportes.

### 2.8 Formación

- Programas y módulos.
- Material y video.
- Participantes.
- Progreso e intentos.
- Banco de preguntas.
- Calificación real según respuestas y ponderación.
- Aprobación por puntaje mínimo.
- Constancia con código verificable, consulta pública de datos mínimos y PDF.
- Creación de capacitación desde una decisión RPM aprobada.

## 3. Persistencia y migración

La aplicación usa `spring.jpa.hibernate.ddl-auto=update`. Para bases existentes se suministra una migración explícita e idempotente:

```text
database/migrations/2026_08_rpm_soa_formacion_integral.sql
```

La migración:

- extiende roles, capacitaciones y constancias;
- crea las nuevas tablas;
- inserta los 93 controles de trabajo;
- inicializa perfiles y SoA para servicios existentes.

## 4. Decisión sobre Machine Learning

No se incorporó un algoritmo de ML sin datos suficientes. El motor determinista permite validar primero la arquitectura inmunoinspirada y crear datos auditables. La memoria RPM almacena situación, prioridad inicial/final, acción, resultado y efectividad. Esa información será la base de un futuro dataset.

Cuando haya suficientes decisiones validadas, CRISP-DM permitirá definir si el ML debe realizar clasificación de prioridad, ranking de casos, recomendación de respuesta o recuperación de casos similares. La selección de algoritmo y métricas dependerá de esa definición y de la calidad real de los datos.

## 5. Limitaciones conscientes

- El catálogo contiene paráfrasis y no sustituye las normas licenciadas.
- La relevancia contextual es una recomendación, no una determinación automática de aplicabilidad.
- RPM apoya la decisión y no reemplaza la evaluación formal de riesgos ni al responsable del SGSI.
- La integración empresarial a gran escala, SSO, almacenamiento en nube y notificaciones por correo quedan como evolución posterior.
- Las pruebas de integración requieren MySQL o Docker en el entorno donde se ejecute el proyecto.

## 6. Validaciones incluidas

- Compilación de 140 fuentes Java y 162 clases generadas.
- Arranque del contexto Spring y verificación de consultas derivadas de repositorios.
- Sintaxis de los archivos JavaScript.
- Correspondencia de IDs y eventos entre HTML y JavaScript.
- Coincidencia exacta de los 93 controles entre CSV y migración.
- Revisión de las 15 tablas agregadas por la migración integral.

Ejecute `./scripts/validate-project.sh` para repetir las comprobaciones estáticas. Las pruebas end-to-end deben realizarse con MySQL/Docker.
