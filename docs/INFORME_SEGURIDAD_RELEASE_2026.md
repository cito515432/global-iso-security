# Informe de seguridad y estado de liberación

**Sistema:** Global ISO Security  
**Fecha de revisión:** 21 de octubre de 2026  
**Repositorio:** [cito515432/global-iso-security](https://github.com/cito515432/global-iso-security)  
**Cambio evaluado:** PR [#47](https://github.com/cito515432/global-iso-security/pull/47), rama `codex/security-controls-2026-10`  
**Commit revisado:** `2c6ed9dccd549baaa0e3e93f50aa29e1c77f1cdf`  
**Ejecución CI:** [Security CI #73](https://github.com/cito515432/global-iso-security/actions/runs/39645679512)

## 1. Dictamen

Los controles descritos en este informe están implementados en el código del PR y los seis trabajos de CI del commit revisado terminaron correctamente. En la versión que actualmente está desplegada también se comprobaron los endpoints de salud y la presentación pública de inicio de sesión.

La nueva versión aún **no está desplegada en producción** y el PR sigue abierto como borrador. Por ello, no se puede certificar que todos los controles nuevos estén activos en producción ni afirmar que ya se cumplan allí los requisitos operativos. La migración V2 de TiDB no se ejecutó: el entorno de trabajo no cuenta con credenciales de TiDB ni con una referencia verificable de un backup restaurable. La activación del restablecimiento por correo y del análisis antivirus también depende de configuración productiva que todavía no se ha comprobado.

Este informe es una revisión técnica del código, CI y señales de producción accesibles; no es una certificación, una prueba de penetración ni una garantía de seguridad total.

## 2. Evidencia de producción revisada

Los servicios activos de Render continúan en la rama `main`, con despliegue automático desactivado. Sus despliegues vigentes son anteriores al PR #47:

| Componente | Commit actualmente desplegado | Verificación realizada |
|---|---|---|
| Backend | `879e7d34364e0e5faff043fd0d56885f4a78b23a` | `/health` respondió `{"status":"ok"}`; `/readiness` respondió `{"status":"ready"}` |
| Frontend | `d7eb897e90e30e8eb29a24c30fd1931c5818f4dc` | La página de acceso cargó y se inspeccionó visualmente en escritorio |
| ML | `40575fd43cd1d541a2bfcf19581c4aa4ba51d4ee` | `/health` respondió `status=ok`, `model_loaded=true`, `RPM-ML-RF-HUMANO-V1` y 20 variables |

La pantalla de acceso observada presenta el formulario de correo y contraseña, el enlace separado de recuperación MFA y los textos informativos del sistema. La composición se ve ordenada en el tamaño de escritorio probado. No se hizo una auditoría visual de todos los paneles autenticados ni de móvil después del PR, porque ese código todavía no está desplegado.

La página gratuita de Render mostró una pantalla de arranque en frío antes de cargar el frontend y el servicio ML. Después de completar el arranque, los endpoints respondieron correctamente. Esto es un comportamiento de disponibilidad inicial del plan actual, no una prueba de rendimiento bajo carga.

## 3. Verificación automatizada del PR

La ejecución CI #73 finalizó en `success`; pasaron los seis trabajos:

| Trabajo | Alcance de la comprobación |
|---|---|
| `backend-build` | Compilación y pruebas automatizadas del backend |
| `backend-packaged-smoke` | Arranque del JAR empaquetado y comprobación de liveness/readiness |
| `backend-mysql-integration` | Integración con MySQL 8 aislado; creación de servicio y SoA, incluida reparación de 92 a 93 controles |
| `tidb-migrator-build` | Empaquetado del migrador controlado de TiDB |
| `ml-validation` | Compilación Python, comprobación de salud/protección por API key y `pip-audit` |
| `dependency-sca / osv-scan` | Análisis de dependencias cubiertas por el workflow |

CI no se conecta a TiDB productiva ni acredita que la migración haya sido aplicada. Tampoco sustituye DAST, pruebas de penetración, análisis de infraestructura o pruebas visuales autenticadas.

## 4. Controles implementados y funcionamiento

### 4.1 Inicio de sesión y fuerza bruta

El servicio mantiene contadores compartidos en base de datos, identificados mediante un digest en vez de guardar directamente la clave de agrupación. La política configurada bloquea después de cinco fallos dentro de una ventana de 15 minutos y mantiene el bloqueo por 15 minutos. No agrega una espera artificial a cada petición. El estado compartido evita depender únicamente de la memoria de una instancia.

El backend nuevo necesita la tabla `login_attempt_buckets` creada por V2. Antes de esa migración el control no puede operar correctamente y el login nuevo puede fallar por falta de esquema; por eso el orden obligatorio es migrar, verificar y luego desplegar backend.

### 4.2 Política y almacenamiento de contraseñas

Las contraseñas nuevas o modificadas exigen al menos ocho caracteres, una mayúscula, una minúscula, un número y un carácter especial; el máximo configurado es 128 caracteres. Antes de BCrypt se aplica un prehash SHA-256 para evitar el límite de bytes de BCrypt; el costo configurado es 12. Las contraseñas ya existentes no se reescriben en masa.

### 4.3 Recuperación de cuenta y MFA

El inicio de sesión habitual conserva correo y contraseña y no solicita un código MFA en cada acceso. La recuperación explícita de cuenta conserva su desafío MFA separado. Para el restablecimiento por correo, el servidor genera un token aleatorio de un solo uso, guarda únicamente su hash, le asigna 15 minutos de vigencia, devuelve mensajes genéricos para reducir la enumeración de cuentas y revoca sesiones JWT al completar el cambio.

El envío requiere migración V2 y SMTP funcional. `PASSWORD_RESET_ENABLED` aparece desactivado por defecto en el Blueprint; la operación productiva no se debe anunciar como disponible hasta probar el envío y el uso de un enlace real.

### 4.4 Roles y autorización

La función de roles personalizados vincula cada rol con uno de los perfiles operativos permitidos. No permite convertir un rol personalizado en administrador. La autorización se valida en el backend, además de ocultar o mostrar módulos en la interfaz; ocultar un botón por sí solo no se considera control de acceso.

Se añadieron pruebas para el mapeo de perfiles y la creación de roles. La prueba visual de punta a punta en la aplicación productiva queda pendiente hasta desplegar la versión.

### 4.5 Aislamiento por empresa

Las operaciones que consultan información empresarial verifican que la empresa solicitada pertenezca al usuario autenticado o que la operación corresponda a un administrador autorizado. Se añadieron pruebas para acceso a empresa asignada y rechazo de acceso cruzado o sin empresa asignada. La validación de integración de CI se ejecuta sobre MySQL 8 aislado; falta el smoke test con dos tenants reales controlados después de desplegar.

### 4.6 Archivos PDF y Excel

La carga conserva validaciones de extensión permitida, tipo MIME, firma, estructura de contenedores Office, tamaño y ubicación de almacenamiento. El nuevo flujo transmite el archivo al escáner ClamAV mediante `INSTREAM` antes de guardar la evidencia. Cuando el escáner requerido falta o no responde, la operación falla de forma cerrada y rechaza la carga.

El Blueprint exige `AV_REQUIRED=true` y solicita `AV_CLAMD_HOST`, pero no aprovisiona por sí mismo un servicio ClamAV. En consecuencia, un escáner privado accesible debe existir y configurarse para que la carga funcione; sin él, el resultado esperado es rechazo, no una carga sin analizar. El escaneo reduce riesgo, pero no detecta necesariamente todo archivo dañino.

### 4.7 Registros de auditoría

Se registran eventos seleccionados de inicio de sesión, restablecimiento de contraseña y cambios de cuenta/roles. La consulta de auditoría se restringe al perfil administrador y devuelve una cantidad limitada de registros recientes. La cobertura no es exhaustiva y los registros no son inmutables frente a un administrador de base de datos; no deben presentarse como un SIEM o almacenamiento de evidencia a prueba de alteración.

### 4.8 SQL injection, sesiones y enlaces

El acceso habitual a datos de negocio usa repositorios JPA; las operaciones JDBC añadidas para límites de intentos, MFA y restablecimiento usan parámetros. La revisión de código de esta entrega no encontró consultas nativas JPA ni concatenación de entrada del usuario en consultas SQL en el código principal revisado. Esto reduce la exposición a inyección SQL en esas rutas, pero no equivale a una prueba DAST completa.

La autenticación utiliza JWT y BCrypt, y los tokens de navegador se guardan en almacenamiento limitado a la sesión con limpieza de valores heredados. La utilidad de enlaces del frontend acepta HTTPS o rutas seguras dentro de la aplicación y rechaza protocolos activos como `javascript:` y `data:`. El secreto JWT no tiene fallback débil en la configuración del backend.

### 4.9 Dependencias, configuración y ransomware

El pipeline ejecuta análisis de dependencias para los manifiestos configurados, incluyendo `pip-audit` para Python y OSV para dependencias cubiertas. Estos resultados son una comprobación con bases de datos conocidas en el momento de la ejecución; no garantizan que no aparezcan vulnerabilidades nuevas.

El producto no puede garantizar por sí solo prevención de ransomware. Esta revisión no verificó una recuperación de backup de TiDB, EDR en endpoints, controles de red de la organización ni un plan de respuesta a incidentes. Los backups restaurables, mínimo privilegio, copias aisladas y monitoreo deben validarse en la operación de la infraestructura.

## 5. Estado frente a requisitos

| Requisito | En el código/CI del PR | En producción actual |
|---|---|---|
| Contraseña robusta | Implementado y probado | No confirmado para el commit pendiente |
| Bloqueo por intentos fallidos | Implementado; depende de tabla V2 | No desplegado ni migrado |
| Recuperación de contraseña olvidada | Implementada; depende de V2 y SMTP | Desactivada/no verificada |
| Crear roles desde la aplicación | Implementado con perfiles limitados | No desplegado ni verificado visualmente |
| Inspección antimalware de PDF/Excel | Implementada con ClamAV y fail-closed | Endpoint privado no acreditado; cargas nuevas se deben probar |
| Autorización por roles y empresa | Implementada/reforzada y probada en servicios | No hay prueba E2E posterior al despliegue nuevo |
| Auditoría | Eventos seleccionados implementados | No se ha verificado la cobertura nueva en vivo |
| Salud del backend y modelo ML actuales | — | Verificados en la versión vigente |
| Auditoría visual completa de dashboards | — | Pendiente; solo se revisó el login público actual |

## 6. Bloqueos de liberación y secuencia segura

1. Verificar que exista un backup de TiDB restaurable y registrar su referencia operativa.
2. Ejecutar el migrador en modos de solo lectura `info` y `validate`; revisar el destino y el historial. Solo entonces aplicar V1/V2, usando el procedimiento y confirmaciones de `database/tidb-migrator/README.md`. No se deben compartir claves en el chat ni escribirlas en el repositorio.
3. Confirmar en el gestor de secretos de Render los valores requeridos para JWT, integración ML, cifrado MFA y configuración DB, sin imprimirlos en logs.
4. Configurar y probar SMTP antes de habilitar recuperación por correo.
5. Disponer de un ClamAV privado accesible por la aplicación y conservar `AV_REQUIRED=true`. El servicio debe estar aislado de la red pública y contar con recursos suficientes.
6. Hacer merge del PR con CI verde y desplegar backend y frontend en orden controlado; desplegar ML solo si el commit contiene cambios aplicables a ese servicio. Render tiene auto-deploy desactivado para los tres servicios del proyecto.
7. Esperar a que cada deploy llegue a `live`. Probar liveness/readiness, inicio normal, bloqueo, recuperación con correo, rol personalizado, autorización entre tenants, auditoría y carga de archivos benignos con escáner disponible y no disponible.
8. Auditar la interfaz autenticada en escritorio y móvil con cuentas de prueba de cada rol, revisar consola y errores de red, y conservar evidencia de los resultados. No usar intentos repetidos contra cuentas reales para verificar bloqueo.
9. Registrar commit y deploy IDs, estado de migración, pruebas ejecutadas y decisión de rollback. Un rollback de código no revierte una migración aplicada.

## 7. Conclusión

La rama del PR contiene la implementación y las pruebas automatizadas de los requisitos de endurecimiento descritos. La versión de producción consultada está saludable en sus endpoints públicos y el acceso se presenta correctamente en escritorio; el modelo ML activo también reporta carga correcta. Sin embargo, por la dependencia del esquema V2 y las configuraciones SMTP/ClamAV aún no verificadas, los controles nuevos no pueden declararse cumplidos en producción todavía. La conclusión correcta es **cumplimiento de implementación en código y CI, con liberación y aceptación operativa pendientes**.
