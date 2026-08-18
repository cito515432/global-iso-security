# Global ISO Security — SoA, riesgos y modelo bioinspirado RPM

Aplicación web para apoyar la construcción y seguimiento de la **Declaración de Aplicabilidad (SoA)** de un Sistema de Gestión de Seguridad de la Información, integrando gestión de riesgos, evidencias, auditoría, formación y un motor bioinspirado RPM explicable.

Esta versión evoluciona el prototipo original hacia un flujo multiempresa funcional. La aplicación web es el medio tecnológico; el aporte investigativo está en la representación, detección, priorización, coordinación humana, evaluación y memoria del modelo RPM.

## Funcionalidades principales

- Catálogo maestro de **93 controles de referencia** de ISO/IEC 27001:2022, con textos de trabajo parafraseados.
- SoA independiente por servicio/organización: aplicabilidad, justificación, estado, porcentaje, responsable y fecha objetivo.
- Perfil organizacional persistente: sector, tamaño, nube, teletrabajo, datos sensibles, pagos, proveedores, servicio 24x7, menores y OT/IoT.
- Recomendación contextual de controles sin excluirlos automáticamente por sector.
- Registro de riesgos y relación riesgo–control.
- Carga, descarga, hash SHA-256, vigencia y validación de evidencias.
- Hallazgos de auditoría, recurrencia y cierre.
- Motor RPM determinista y explicable con antígenos, señales de peligro, prioridad, respuesta, validación y memoria.
- Portal ejecutivo para la organización.
- Formación completa: programas, módulos, participantes, preguntas, intentos, calificación y constancias verificables mediante código público sin exponer documentos de identidad.
- Recomendaciones RPM que pueden convertirse en acciones formativas.
- Reportes PDF y Excel con SoA, riesgos y resultados RPM.
- Autenticación JWT, BCrypt, autorización por rol y aislamiento por empresa.
- Persistencia real en MySQL; las evidencias se conservan en un volumen de Docker.

## Arquitectura tecnológica

- **Backend:** Java 20, Spring Boot 3.2, Spring Security, JWT, JPA/Hibernate, MySQL.
- **Frontend:** HTML5, CSS3 y JavaScript sin framework.
- **Despliegue local:** Docker Compose con MySQL, backend y Nginx.
- **Archivos:** volumen persistente `evidence_data`.

```text
frontend por rol
      │
      ▼
API REST Spring Boot
      │
      ├── SoA ── Riesgos ── Evidencias ── Hallazgos
      ├── Formación ── Evaluaciones ── Constancias
      └── Motor RPM ── Decisiones humanas ── Memoria
      │
      ▼
MySQL + almacenamiento persistente de evidencias
```

## Roles

| Rol | Responsabilidad principal |
|---|---|
| `ADMINISTRADOR` | Empresas, usuarios, servicios, catálogo, roles y reportes. |
| `IMPLEMENTADOR` | Contexto, SoA, riesgos, evidencias y ejecución del análisis RPM. |
| `AUDITOR` | Validación de evidencias, hallazgos, firmas y decisiones RPM. |
| `CAPACITADOR` | Programas, módulos, participantes, evaluaciones, constancias y respuestas formativas RPM. |
| `USUARIO_EMPRESA` | Portal ejecutivo, progreso, riesgos, decisiones, formación y reportes de su organización. |

La matriz completa se encuentra en [`docs/MATRIZ_ROLES_PERMISOS.md`](docs/MATRIZ_ROLES_PERMISOS.md).

## Ejecución rápida con Docker

### 1. Preparar variables

```bash
cp .env.example .env
```

Cambie, como mínimo, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET` y `SEED_ADMIN_PASSWORD`. Use `SEED_ENABLED=false` para impedir cualquier carga automática en un entorno controlado y `SEED_DEMO_DATA=false` fuera de demostraciones.

### 2. Iniciar

```bash
docker compose up --build
```

Abra:

```text
http://localhost:8080/pages/login.html
```

La base inicial se importa desde `database/init/01_globalisosecurity_backup.sql` únicamente al crear por primera vez el volumen MySQL. Las tablas nuevas también pueden crearse mediante Hibernate (`ddl-auto=update`). Para una migración controlada sobre una base existente use:

```text
database/migrations/2026_08_rpm_soa_formacion_integral.sql
```

### Reinicio limpio de base de datos

> Este comando elimina los datos locales del volumen MySQL.

```bash
docker compose down -v
docker compose up --build
```

## Cuentas académicas de demostración

Se crean de forma idempotente cuando `SEED_DEMO_DATA=true`:

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin@globalisosecurity.com` | valor de `SEED_ADMIN_PASSWORD` |
| Implementador | `implementador@demo.com` | `Demo123*` |
| Auditor | `auditor@demo.com` | `Demo123*` |
| Capacitador | `capacitador@demo.com` | `Demo123*` |
| Empresa | `empresa@demo.com` | `Demo123*` |

Desactive las semillas y cambie todas las credenciales antes de un despliegue real.

## Motor bioinspirado RPM

La adaptación toma como referencia principal el marco inmunoinspirado de Darmoul, Pierreval y Hajri-Gabouj para gestionar disrupciones. El proyecto traslada sus funciones de **detectar, reaccionar, coordinar y evaluar** al análisis de condiciones relacionadas con la SoA.

```text
Controles, riesgos y evidencias ──► células y tejido artificial
Desviaciones observadas          ──► antígenos
Situación de riesgo              ──► patógeno
Consecuencias/contexto           ──► señales de peligro
Alternativas de respuesta        ──► células B / anticuerpos
Validación por especialistas     ──► células Th
Casos y resultados históricos   ──► memoria inmunológica
```

El motor actual es **determinista, trazable y explicable**. No se incorporó de forma arbitraria un algoritmo de Machine Learning. La tabla `rpm_memoria` conserva las variables, decisiones y resultados que permitirán construir posteriormente un conjunto de datos y aplicar CRISP-DM con una función de ML justificada.

Véase [`docs/ARQUITECTURA_RPM.md`](docs/ARQUITECTURA_RPM.md).

## Catálogo de controles y derechos de uso

El archivo `backend/src/main/resources/data/iso27001_controls.csv` contiene códigos, títulos cortos, descripciones y preguntas **parafraseadas para fines académicos y operativos**. No es una copia ni reemplaza el texto oficial de ISO/IEC 27001 o ISO/IEC 27002. Para una implementación comercial o certificable debe cargarse contenido autorizado/licenciado y ser revisado por un profesional competente.

## Estructura relevante

```text
global-iso-security/
├── backend/
│   ├── src/main/java/.../controllers
│   ├── src/main/java/.../models
│   ├── src/main/java/.../services
│   └── src/main/resources/data/iso27001_controls.csv
├── frontend/
│   ├── pages/admin.html
│   ├── pages/implementador.html
│   ├── pages/auditor.html
│   ├── pages/capacitador.html
│   ├── pages/empresa.html
│   └── js/
├── database/
│   ├── init/
│   └── migrations/2026_08_rpm_soa_formacion_integral.sql
├── docs/
└── docker-compose.yml
```

## Documentación

- [`IMPLEMENTACION_RPM_INTEGRAL.md`](IMPLEMENTACION_RPM_INTEGRAL.md): inventario funcional y decisiones de implementación.
- [`docs/ARQUITECTURA_RPM.md`](docs/ARQUITECTURA_RPM.md): capas, entradas, salidas y trazabilidad del modelo.
- [`docs/GUIA_PRUEBAS.md`](docs/GUIA_PRUEBAS.md): pruebas funcionales y demostración integral por roles.
- [`docs/MATRIZ_ROLES_PERMISOS.md`](docs/MATRIZ_ROLES_PERMISOS.md): permisos y responsabilidades.
- [`docs/CAMBIOS_VERSION_RPM.md`](docs/CAMBIOS_VERSION_RPM.md): cambios frente al prototipo original.

## Estado de validación de esta entrega

- Compilación estática de las **140 fuentes Java**: realizada.
- Arranque del contexto Spring y análisis de consultas derivadas de repositorios: realizado con una base embebida solo para validación estructural.
- Validación de sintaxis de todos los archivos JavaScript: realizada.
- Verificación cruzada del catálogo CSV y la migración: **93 códigos únicos y coincidentes**.
- Verificación de IDs y controladores de eventos entre las páginas HTML y sus archivos JavaScript: realizada.
- Pruebas de integración completas contra MySQL/Docker: deben ejecutarse en un equipo con Docker o MySQL disponible siguiendo `docs/GUIA_PRUEBAS.md`.

La validación estática puede repetirse con:

```bash
./scripts/validate-project.sh
```

## Equipo académico

- Andrés Felipe Obando Barriga
- María Camila Sarmiento
- Juan Esteban Pardo Bedoya

Universidad de San Buenaventura, sede Bogotá — Ingeniería de Sistemas.
