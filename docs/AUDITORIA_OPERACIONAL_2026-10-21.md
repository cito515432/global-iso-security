# Auditoría operacional y registro de cambios

**Proyecto:** Global ISO Security  
**Fecha de revisión:** 21 de octubre de 2026  
**Repositorio:** https://github.com/cito515432/global-iso-security  
**Versión revisada:** commit 50d604fe6ab27675c3ecefe08b8cf1e9e686e608, PR #47 fusionado  
**Ejecución CI:** Security CI #74, resultado success  
**Servicios:** backend, frontend y ML en Render

## 1. Resumen

Se documenta la implementación de controles de seguridad del PR #47 y la verificación operacional realizada el 21 de octubre de 2026. El backend y el frontend ejecutan el commit 50d604f; el servicio ML continúa en la versión previamente validada. Los tres servicios tienen el despliegue automático desactivado.

En la verificación de producción respondieron correctamente los endpoints de salud y readiness. También se comprobó que las rutas de API protegidas rechazan solicitudes sin autenticación, que el endpoint de predicción ML exige una API key y que la política CORS no acepta un origen ajeno al frontend. Estas comprobaciones confirman disponibilidad básica y controles de borde; no equivalen a una prueba integral de todos los flujos autenticados ni a una certificación de seguridad.

## 2. Requisitos de seguridad y controles implementados

### 2.1 Credenciales y protección ante fuerza bruta

La política para contraseñas nuevas o cambiadas exige un mínimo de ocho caracteres, con mayúscula, minúscula, número y carácter especial. El máximo configurado es de 128 caracteres. El backend prepara la contraseña con SHA-256 antes de BCrypt y utiliza costo 12.

Los intentos fallidos se agrupan en contadores persistentes compartidos entre instancias. La política configurada bloquea tras cinco fallos dentro de una ventana de 15 minutos y mantiene el bloqueo por 15 minutos. No se añade una espera artificial a cada petición. El acceso cotidiano usa correo y contraseña sin exigir MFA en cada inicio.

### 2.2 Recuperación de contraseña

El flujo genera un token aleatorio de un solo uso, conserva su hash, limita su vigencia a 15 minutos, devuelve una respuesta genérica para reducir enumeración de cuentas y revoca sesiones al completar el cambio. La entrega de correo depende de SMTP y de habilitar PASSWORD_RESET_ENABLED; la variable aparece desactivada por defecto en el Blueprint.

### 2.3 Roles y separación por empresa

El administrador puede crear roles personalizados desde la aplicación. Cada rol hereda uno de los perfiles operativos permitidos y no puede elevarse al perfil Administrador. La autorización se verifica en el backend.

Las operaciones de datos de empresa verifican la pertenencia del usuario al tenant solicitado o que la operación corresponda a un administrador autorizado. La interfaz adapta los módulos al rol, pero la decisión de acceso se aplica en el servidor.

### 2.4 Archivos PDF y Excel

La carga valida extensión, tipo, firma, tamaño y estructura esperada del archivo. Antes de almacenar una evidencia, el backend transmite el contenido al escáner ClamAV mediante INSTREAM. Con AV_REQUIRED activo, la ausencia o indisponibilidad del escáner produce rechazo de la carga (fail-closed), no aceptación sin inspección.

### 2.5 Auditoría, sesión y reducción de exposición

Se registran eventos seleccionados de acceso, restablecimiento y cambios de cuenta o roles. La consulta queda restringida al administrador y limitada a registros recientes; este registro no se presenta como SIEM ni como almacenamiento inmutable.

El acceso a datos usa repositorios JPA y las operaciones JDBC añadidas usan parámetros. La revisión del código no encontró concatenación de entrada del usuario en consultas SQL en las rutas revisadas. También se validan los enlaces ingresados por usuarios y el secreto JWT no tiene un fallback débil.

## 3. Pruebas automatizadas

La ejecución Security CI #74 finalizó correctamente con seis trabajos:

| Trabajo | Evidencia |
|---|---|
| backend-build | Compilación y pruebas automatizadas del backend |
| backend-packaged-smoke | Arranque del JAR empaquetado y comprobación de liveness/readiness |
| backend-mysql-integration | Integración contra MySQL aislado; creación del servicio y reparación SoA de 92 a 93 controles |
| tidb-migrator-build | Construcción del migrador controlado para TiDB |
| ml-validation | Compilación Python, salud y protección por API key; auditoría pip-audit |
| dependency-sca / osv-scan | Análisis de dependencias cubiertas por OSV |

La CI no se conecta a la instancia productiva de TiDB ni confirma que una migración se haya aplicado allí. Los análisis de dependencias son preventivos y dependen de las bases de datos disponibles al momento de ejecutarlos.

## 4. Verificaciones de producción del 21 de octubre

| Componente o control | Resultado observado |
|---|---|
| Backend /health | HTTP 200, estado ok |
| Backend /readiness | HTTP 200, estado ready |
| Frontend /api/health | HTTP 200 |
| Servicio ML /health | HTTP 200; modelo RPM-ML-RF-HUMANO-V1 cargado con 20 variables |
| Endpoints backend protegidos | Solicitudes sin autenticación rechazadas con HTTP 403 |
| ML /predict | Solicitud sin API key rechazada con HTTP 401 |
| CORS | Origen del frontend aceptado; origen externo no autorizado rechazado |
| Cabeceras de seguridad | HSTS, nosniff, frame deny y no-referrer observados |
| Disponibilidad observada | Sin respuestas HTTP 5xx en los registros consultados de las últimas 24 horas |

La inspección visual cubrió la página pública de inicio de sesión y la carga HTTP de las páginas por rol. Esto confirma que las rutas se sirven, pero no que un usuario autenticado de cada rol complete todas sus operaciones. No se utilizaron credenciales de producción para una prueba de extremo a extremo por rol.

Se observó que una solicitud de API con token inválido recibe 403. El backend deniega el acceso; el frontend solo borra la sesión automáticamente ante 401, por lo que conviene armonizar las respuestas de autenticación y autorización para mejorar la experiencia cuando una sesión caduca.

## 5. Despliegue y configuración

Los servicios backend y frontend están en Render y ejecutan el commit 50d604fe6ab27675c3ecefe08b8cf1e9e686e608. El servicio ML activo conserva el commit 40575fd43cd1d541a2bfcf19581c4aa4ba51d4ee. El despliegue automático está desactivado en los tres servicios y las instancias revisadas usan el plan gratuito.

En el Blueprint se establece PASSWORD_RESET_ENABLED como false por defecto y las variables de SMTP y del host ClamAV requieren configuración externa. No se incluyen valores secretos en esta documentación. El propietario del proyecto informó que las claves fueron rotadas y el backup fue verificado; esta auditoría no inspeccionó los valores de secretos ni ejecutó una restauración del backup.

## 6. Alcance de las verificaciones operativas

Las verificaciones de hoy confirman disponibilidad pública, rechazo de solicitudes anónimas, protección de la API ML por clave, configuración CORS y cabeceras de seguridad. La CI valida el comportamiento de varias rutas de seguridad en entornos aislados.

Quedan fuera de la evidencia recogida hoy: inicio de sesión autenticado con cada uno de los cinco roles; envío real de un correo SMTP; conexión productiva con ClamAV y análisis de archivos reales; y consulta directa del estado de la migración V2 en TiDB. Por esta razón, la activación operativa de recuperación por correo, el escaneo productivo y todos los recorridos autenticados deben verificarse en sus respectivos entornos antes de comunicarlos como comprobados.

## 7. Conclusión

La versión 50d604f consolida en producción los cambios de código del PR #47. La CI #74 pasó sus seis trabajos y las comprobaciones públicas realizadas en backend, frontend y ML respondieron según lo esperado. Se verificó que las rutas protegidas no exponen datos ante solicitudes anónimas y que la predicción ML requiere API key.

Este informe registra evidencia operacional acotada. No constituye una prueba de penetración ni una certificación de seguridad total. Para los detalles previos al despliegue y el diseño completo de controles, consultar el [informe de seguridad de la liberación](INFORME_SEGURIDAD_RELEASE_2026.md).
