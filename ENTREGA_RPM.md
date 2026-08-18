# Entrega integral — Global ISO Security con modelo RPM

## 1. Resultado

La versión recibida fue evolucionada desde un prototipo basado principalmente en checklist hacia una aplicación académica funcional organizada alrededor de:

```text
Contexto de la organización
        ↓
Registro y valoración de riesgos
        ↓
Declaración de Aplicabilidad con 93 controles
        ↓
Implementación, evidencias y auditoría
        ↓
Modelo bioinspirado RPM explicable
        ↓
Validación humana y acciones de respuesta
        ↓
Formación, seguimiento y memoria
```

No se eliminó el esquema anterior de checklist, porque puede contener información del proyecto original. Se conserva como compatibilidad, pero el flujo principal nuevo utiliza `soa_controles`.

## 2. Funcionalidades incorporadas

### Catálogo y SoA

- Catálogo maestro con 93 controles de referencia.
- Códigos únicos y distribución en dominios organizacionales, de personas, físicos y tecnológicos.
- Textos de trabajo parafraseados; no sustituyen el contenido oficial licenciado de ISO.
- Una SoA independiente para cada servicio/organización.
- Aplicabilidad, justificación, estado de implementación, porcentaje, responsable, fecha objetivo y observaciones.
- Justificación obligatoria para declarar un control no aplicable.
- Recomendaciones contextuales sin ocultar o excluir controles automáticamente.

### Contexto organizacional

- Sector.
- Tamaño.
- Alcance del SGSI.
- Responsable del SGSI.
- Tratamiento de datos sensibles.
- Uso de nube y trabajo remoto.
- Procesamiento de pagos.
- Dependencia de proveedores.
- Operación crítica 24/7.
- Manejo de menores.
- Infraestructura propia y OT/IoT.
- Umbral de aceptación del riesgo.

El sector se utiliza como una variable de recomendación. La aplicabilidad sigue requiriendo análisis y justificación humana.

### Gestión de riesgos

- Código y nombre.
- Activo de información.
- Amenaza, vulnerabilidad y consecuencia.
- Probabilidad e impacto en escala 1–5.
- Nivel inherente y residual.
- Tratamiento, estado, responsable y fecha de revisión.
- Relación formal entre riesgos y controles.
- Eficacia esperada de cada relación de tratamiento.

### Evidencias y auditoría

- Carga multipart de archivos de hasta 25 MB.
- Nombre original e interno.
- Tipo MIME.
- Hash SHA-256.
- Fecha de vencimiento.
- Estado pendiente, validado o rechazado.
- Observación obligatoria al rechazar.
- Descarga controlada según empresa.
- Protección contra acceso a rutas fuera del almacenamiento autorizado.
- Hallazgos relacionados con controles y riesgos.
- Severidad, recurrencia, estado y cierre.

### Modelo bioinspirado RPM

El motor implementa una adaptación explícita de las funciones inmunoinspiradas:

- **Células/tejido:** controles SoA y sus relaciones con contexto, riesgos y evidencias.
- **APC:** comparación entre condiciones esperadas y observadas.
- **Antígenos:** desviaciones, ausencia o rechazo de evidencia, implementación insuficiente, recurrencia y vencimientos.
- **Señales de peligro:** consecuencias y factores que aumentan la prioridad.
- **Células B/anticuerpos:** alternativas de respuesta.
- **Células Th:** validación humana, modificación o rechazo de la respuesta.
- **Memoria:** casos anteriores, decisiones, resultados y efectividad.
- **Evaluación:** reanálisis y observación de consecuencias residuales.

El motor devuelve:

- puntaje de 0 a 100;
- prioridad baja, media, alta o crítica;
- explicación de factores;
- antígenos y señales;
- acciones sugeridas;
- estado de validación;
- casos similares disponibles en memoria.

Esta versión no incorpora un algoritmo de Machine Learning arbitrario. Primero genera memoria auditable; después CRISP-DM permitirá decidir si el problema será clasificación, ranking, recomendación o recuperación de casos.

### Roles

- **Administrador:** empresas, usuarios, roles, servicios, sectores, catálogo, contexto y reportes.
- **Implementador:** SoA, riesgos, relación riesgo-control, evidencias y análisis RPM.
- **Auditor:** evidencias, hallazgos, decisiones RPM, firmas y reportes.
- **Capacitador:** programas, módulos, participantes, preguntas, intentos, resultados, constancias y recomendaciones RPM.
- **Usuario Empresa:** portal ejecutivo propio con progreso, SoA, riesgos, RPM, formación, contexto y reportes.

### Portal de empresa

- Progreso general y por etapas.
- Controles aplicables, implementados y pendientes.
- Riesgos por categoría.
- Evidencias pendientes o rechazadas.
- Alertas RPM.
- Actividades prioritarias explicables.
- Estado de formación.
- Consulta y validación de decisiones permitidas.
- Descarga de reportes PDF y Excel.

### Formación

- Programas reales persistentes.
- Objetivo, público, fechas y puntaje mínimo.
- Módulos con orden, contenido, material y video.
- Participantes y progreso.
- Banco de preguntas.
- Registro de intentos y cálculo de puntaje.
- Aprobación automática según el mínimo configurado.
- Constancia PDF con código verificable.
- Verificación pública de datos mínimos sin exponer el documento de identidad.
- Conversión de una respuesta RPM de capacitación en un programa real.

### Reportes

- Resumen de la organización.
- SoA.
- Riesgos.
- Análisis RPM.
- Exportación PDF y Excel.

## 3. Archivos clave

```text
backend/src/main/resources/data/iso27001_controls.csv
database/migrations/2026_08_rpm_soa_formacion_integral.sql
backend/src/main/java/com/globalisosecurity/backend/services/RpmEngineService.java
backend/src/main/java/com/globalisosecurity/backend/services/PortalEmpresaService.java
backend/src/main/java/com/globalisosecurity/backend/services/FormacionService.java
frontend/pages/empresa.html
frontend/pages/capacitador.html
docs/ARQUITECTURA_RPM.md
docs/MATRIZ_ROLES_PERMISOS.md
docs/GUIA_PRUEBAS.md
scripts/validate-project.sh
```

## 4. Puesta en marcha

```bash
cp .env.example .env
docker compose up --build
```

Acceso:

```text
http://localhost:8080/pages/login.html
```

Para una base nueva, MySQL importa el respaldo de `database/init` y Hibernate completa el esquema. Para una base ya existente se dispone de:

```text
database/migrations/2026_08_rpm_soa_formacion_integral.sql
```

Si se desea reiniciar todo el entorno académico:

```bash
docker compose down -v
docker compose up --build
```

> El primer comando elimina la base local almacenada en el volumen.

## 5. Datos de demostración

Cuando `SEED_DEMO_DATA=true` se crean:

- `implementador@demo.com` / `Demo123*`
- `auditor@demo.com` / `Demo123*`
- `capacitador@demo.com` / `Demo123*`
- `empresa@demo.com` / `Demo123*`

El administrador utiliza `SEED_ADMIN_EMAIL` y `SEED_ADMIN_PASSWORD`.

En un entorno no académico se deben usar:

```text
SEED_DEMO_DATA=false
SEED_ENABLED=false
```

además de secretos y contraseñas robustos.

## 6. Validación realizada

- Compilación de 140 archivos fuente Java.
- Generación de 162 clases Java.
- Arranque del contexto Spring sin errores de consultas derivadas de repositorios.
- Detección de 148 rutas de controladores sin colisiones de mapeo.
- Validación de sintaxis de todos los JavaScript.
- Verificación de IDs y handlers entre HTML y JavaScript.
- 93 controles únicos en CSV.
- 93 controles coincidentes en la migración.
- 15 tablas integrales verificadas en la migración.
- JAR ejecutable actualizado con las nuevas clases y recursos.

La validación estática puede repetirse con:

```bash
./scripts/validate-project.sh
```

## 7. Limitaciones honestas

- En el entorno de construcción no estaba disponible Docker ni un servidor MySQL; por ello no se afirma haber ejecutado el flujo end-to-end contra MySQL. La guía detallada está en `docs/GUIA_PRUEBAS.md`.
- El catálogo contiene paráfrasis académicas y no reemplaza la licencia ni el texto oficial de ISO/IEC 27001 o ISO/IEC 27002.
- La relevancia contextual es una recomendación. No determina por sí sola que un control sea aplicable o no aplicable.
- RPM apoya la decisión y no reemplaza la metodología formal de gestión de riesgos ni al responsable del SGSI.
- Para producción todavía conviene incorporar SSO/MFA, almacenamiento de objetos, antivirus de archivos, notificaciones, backups administrados, monitoreo y pruebas de penetración completas.

## 8. Siguiente validación recomendada

Ejecutar el escenario de `docs/GUIA_PRUEBAS.md` y conservar:

- capturas por rol;
- registros de API;
- evidencia de persistencia;
- exportaciones PDF/Excel;
- videos de cada prueba;
- tabla de resultados esperados/obtenidos;
- decisiones RPM y memoria resultante.

Esto permitirá alimentar el Capítulo 2, el Capítulo 3 y los anexos de la tesis con evidencia real del software.
