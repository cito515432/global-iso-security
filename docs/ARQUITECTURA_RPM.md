# Arquitectura bioinspirada RPM

## 1. Fuente conceptual principal

La arquitectura usa como referencia principal el artículo **“Handling disruptions in manufacturing systems: An immune perspective”**, de Saber Darmoul, Henri Pierreval y Sonia Hajri-Gabouj. El artículo propone un marco con células artificiales, tejido, patógenos, antígenos, células presentadoras de antígenos (APC), señales de peligro, células B, células Th y memoria.

El artículo estudia sistemas de manufactura. No define la Declaración de Aplicabilidad, ISO/IEC 27001 ni el nombre RPM. Por tanto, la correspondencia descrita aquí es una adaptación del proyecto que debe evaluarse empíricamente.

## 2. Problema que aborda RPM

RPM ayuda a responder:

> ¿Qué condiciones relacionadas con los controles de la SoA requieren atención primero, por qué y qué respuesta puede someterse a validación humana?

No calcula el riesgo oficial de la organización ni reemplaza la metodología de gestión de riesgos. Integra información dispersa y genera una priorización explicable para apoyar la decisión.

## 3. Origen de los datos

| Fuente | Variables principales |
|---|---|
| Perfil organizacional | sector, tamaño, datos sensibles, nube, remoto, pagos, 24x7, menores, OT/IoT, umbral de aceptación |
| SoA | aplicabilidad, estado, porcentaje, responsable, fecha objetivo, observaciones |
| Riesgos | probabilidad, impacto, nivel inherente/residual, tratamiento, estado |
| Evidencias | existencia, vigencia, estado de validación, hash y fecha |
| Hallazgos | severidad, recurrencia, estado y relación con control/riesgo |
| Formación | progreso, puntajes, aprobaciones e intentos |
| Memoria RPM | decisiones, acciones, resultados y efectividad de casos anteriores |

## 4. Capas implementadas

### 4.1 Representación y contexto

- **Célula artificial:** registro `SoaControl`, enriquecido con riesgo, evidencia, hallazgo y contexto.
- **Tejido artificial:** relaciones entre servicios, controles, riesgos, evidencias, hallazgos y formación.
- **Condición normal esperada:** control aplicable, implementación suficiente, evidencia válida, riesgo tratado y ausencia de hallazgos críticos vencidos.

### 4.2 Detección y clasificación

Las reglas APC comparan el estado observado con la condición esperada. Las desviaciones se convierten en antígenos, por ejemplo:

- control aplicable no iniciado;
- implementación parcial o no efectiva;
- falta de evidencia;
- evidencia rechazada o vencida;
- fecha objetivo vencida;
- hallazgo abierto o recurrente;
- riesgo alto o crítico.

El conjunto de antígenos caracteriza el “patógeno”, entendido como la situación que puede afectar el desempeño esperado del SGSI.

### 4.3 Propagación del peligro

Las señales de peligro expresan consecuencias y contexto. No se limitan al incumplimiento individual: aumentan su peso cuando concurren riesgo alto, recurrencia, datos sensibles, operación crítica u otras relaciones.

### 4.4 Reacción y priorización

El motor suma pesos trazables y normaliza un puntaje entre 0 y 100:

| Rango | Prioridad |
|---:|---|
| 0–24 | Baja |
| 25–49 | Media |
| 50–74 | Alta |
| 75–100 | Crítica |

Las células B se implementan como mecanismos de decisión. Sus anticuerpos son acciones sugeridas, entre ellas:

- revisar o completar evidencia;
- construir plan de implementación;
- tratar un riesgo;
- crear una capacitación;
- escalar al responsable;
- mantener monitoreo.

### 4.5 Coordinación y validación humana

Las decisiones no se ejecutan automáticamente. Implementador, auditor, responsable de la empresa o capacitador —según el caso— pueden aprobar, modificar o rechazar la sugerencia y registrar una justificación. Esta interacción representa la función coordinadora de las células Th.

### 4.6 Memoria y adaptación

`RpmMemoria` conserva:

- huella del caso;
- situación de entrada;
- prioridad inicial y final;
- acción aplicada;
- resultado;
- porcentaje de efectividad.

La huella permite buscar casos similares. En esta versión, la memoria aporta trazabilidad y coincidencia exacta/estructurada; no se presenta como aprendizaje automático ya entrenado.

### 4.7 Evaluación

El resultado se registra después de ejecutar la respuesta. El evaluador puede indicar efectividad y consecuencias residuales. Esto permite cerrar el ciclo y preparar datos para análisis cuantitativo posterior.

## 5. Funciones del marco

```text
DETECTAR
Células + APC + antígenos + patógeno
        ↓
REACCIONAR
Señales + células B + acciones
        ↓
COORDINAR
Células Th + validación por roles
        ↓
EVALUAR
Consecuencias residuales + resultado
        ↓
MEMORIZAR
Caso, respuesta y efectividad
```

## 6. Trazabilidad en código

| Concepto | Implementación principal |
|---|---|
| Célula/tejido | `SoaControl`, `RiesgoControl`, `Evidencia`, `HallazgoAuditoria` |
| APC/detección | `RpmEngineService` |
| Antígeno/señal | `RpmSenal` con categorías y pesos |
| Patógeno/caso | `RpmAnalisis` |
| Célula B/anticuerpo | `RpmDecision` |
| Célula Th | endpoint de validación de decisiones y roles humanos |
| Memoria | `RpmMemoria` |
| Acción formativa | `FormacionService` y capacitación creada desde RPM |

## 7. Integración futura con Machine Learning

La arquitectura deja preparado un pipeline CRISP-DM:

1. Comprensión: priorizar condiciones de atención o recomendar respuestas.
2. Datos: análisis, señales, contexto, decisiones y resultados validados.
3. Preparación: depuración de memoria, codificación de variables y control de sesgo.
4. Modelado: comparación de algoritmos después de definir la variable objetivo.
5. Evaluación: métricas acordes con clasificación, ranking o recomendación.
6. Despliegue: uso del modelo como un B-cell adicional, manteniendo explicación y validación humana.
