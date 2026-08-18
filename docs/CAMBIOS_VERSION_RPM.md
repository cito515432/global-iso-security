# Cambios de la versión RPM integral

## Antes

- Checklist reducido y repetido por sector.
- Sector usado principalmente como dato visual.
- Varias pantallas y métricas con comportamiento simulado.
- Sin registro formal de riesgos ni relación riesgo–control.
- Evidencias sin ciclo completo de archivo y validación.
- Sin portal propio para la organización.
- Capacitador con pocos módulos estáticos.
- Modelo bioinspirado principalmente conceptual.

## Ahora

### Base de datos

Se añadieron entidades para perfil organizacional, catálogo, SoA, riesgos, relaciones, evidencias, hallazgos, RPM, formación e intentos. La migración integral está en `database/migrations/2026_08_rpm_soa_formacion_integral.sql`.

### SoA

Los 93 controles se inicializan para cada servicio. El sector/contexto produce recomendaciones, no exclusiones automáticas. Cada decisión de aplicabilidad es editable y justificable.

### Riesgos y evidencias

La aplicación puede registrar riesgos reales, asociarlos a controles y cargar archivos con hash. El auditor valida o rechaza evidencias y registra hallazgos.

### RPM

El motor ya existe como servicio de backend. Genera señales, prioridad, explicación y acciones. Las decisiones se someten a validación humana y pueden convertirse en memoria.

### Roles y frontend

Se renovaron las páginas de administrador, implementador, auditor y capacitador. Se añadió `empresa.html` con su propia redirección desde login. Todos los dashboards consumen datos reales del backend.

### Formación

El capacitador administra cursos, módulos, participantes, preguntas, intentos, calificaciones y constancias. Una decisión RPM aprobada puede generar una capacitación asociada al control o riesgo que la originó.

### Seguridad

Se reforzó la separación por empresa, se restringieron endpoints por rol y se conservaron JWT, BCrypt y logs. Los flujos legados de checklist/evaluación quedaron protegidos y se mantienen solo por compatibilidad.

## Compatibilidad

El esquema legado no se elimina. Las tablas `checklists`, `items_checklist` y `evaluaciones` se conservan, pero el flujo principal usa `soa_controles`. Esto permite migrar gradualmente sin perder la información previa.
