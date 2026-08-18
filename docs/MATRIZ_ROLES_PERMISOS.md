# Matriz de roles y permisos

| Función | Administrador | Implementador | Auditor | Capacitador | Empresa |
|---|:---:|:---:|:---:|:---:|:---:|
| Crear empresas y servicios | Sí | No | No | No | No |
| Gestionar usuarios y roles | Sí | No | No | No | No |
| Consultar catálogo de 93 controles | Sí | Sí | Sí | Sí* | Sí* |
| Editar perfil organizacional | Sí | Sí | No | No | Consulta |
| Inicializar/editar SoA | Sí | Sí | Consulta | No | Consulta |
| Crear/editar riesgos | Sí | Sí | Consulta | No | Consulta |
| Cargar evidencias | Sí | Sí | No | No | Consulta |
| Validar evidencias | Sí | No | Sí | No | No |
| Crear/cerrar hallazgos | Sí | No | Sí | No | Consulta |
| Ejecutar análisis RPM | Sí | Sí | Sí | No | No |
| Validar decisión RPM | Sí | Sí | Sí | Sí** | Sí |
| Registrar memoria/resultado | Sí | Sí | Sí | No | Sí |
| Crear capacitación | Sí | No | No | Sí | No |
| Convertir respuesta RPM en capacitación | Sí | No | No | Sí | No |
| Gestionar módulos/preguntas/participantes | Sí | No | No | Sí | No |
| Ver progreso de formación | Sí | Sí* | Sí* | Sí | Sí |
| Emitir constancias | Sí | No | No | Sí | Consulta/descarga |
| Firmar proceso | Sí | Sí | Sí | No | Sí |
| Descargar reportes | Sí | Sí | Sí | Sí* | Sí |

\* Lectura limitada a la organización autorizada o a los datos requeridos por su función.  
\** El capacitador valida principalmente decisiones RPM de tipo `CAPACITACION`.

## Reglas de aislamiento

- Un usuario con empresa asignada solo puede consultar datos de esa empresa.
- Un administrador puede operar globalmente.
- Los roles internos sin empresa asignada pueden actuar como equipo consultor global; si tienen empresa, quedan limitados a ella.
- La autorización de interfaz no reemplaza la autorización del backend: los endpoints críticos usan `@PreAuthorize` y validación de empresa/servicio.
