# Guía de pruebas funcionales e integrales

## 1. Preparación

```bash
cp .env.example .env
docker compose up --build
```

Acceso:

```text
http://localhost:8080/pages/login.html
```

Compruebe salud del backend:

```bash
curl http://localhost:8080/api/health
```

## 2. Prueba de autenticación y roles

1. Iniciar sesión con cada cuenta demo.
2. Confirmar redirección correcta:
   - Administrador → `admin.html`.
   - Implementador → `implementador.html`.
   - Auditor → `auditor.html`.
   - Capacitador → `capacitador.html`.
   - Empresa → `empresa.html`.
3. Intentar abrir manualmente una página ajena y confirmar que la sesión se redirige.
4. Probar un endpoint de escritura con rol no autorizado y confirmar HTTP 403.

## 3. Catálogo y SoA

1. Administrador: confirmar 93 controles únicos.
2. Crear empresa y servicio.
3. Implementador: abrir SoA y comprobar que se crean 93 registros.
4. Cambiar un control a `NO_APLICABLE` sin justificación: debe fallar.
5. Justificarlo: debe guardar.
6. Marcar otro como `APLICABLE`, `PARCIAL`, 40 %, responsable y fecha objetivo.
7. Confirmar que el dashboard refleja el cambio.

## 4. Contexto sectorial

1. Editar perfil como empresa de Salud o Educación.
2. Activar datos sensibles, nube, remoto u otras variables.
3. Reinicializar/recalcular SoA.
4. Verificar que cambian puntaje y explicación de relevancia.
5. Confirmar que ningún control desaparece automáticamente.

## 5. Riesgos

1. Crear riesgo con activo, amenaza, vulnerabilidad, consecuencia, probabilidad e impacto.
2. Relacionarlo con uno o más controles.
3. Confirmar cálculo de nivel inherente.
4. Editar nivel residual y tratamiento.
5. Verificar que aparece en portal de empresa y reporte.

## 6. Evidencias

1. Cargar archivo a un control como implementador.
2. Descargar y verificar que coincide con el original.
3. Auditor: validar o rechazar con observación.
4. Cargar evidencia con fecha vencida y confirmar que RPM la detecta.
5. Reiniciar contenedores sin eliminar volúmenes y confirmar persistencia del archivo.

## 7. Hallazgos

1. Auditor: crear hallazgo relacionado con control/riesgo.
2. Marcarlo recurrente y con severidad alta.
3. Ejecutar RPM y comprobar que aporta una señal con peso explicable.
4. Cerrar el hallazgo y volver a analizar; la huella/resultado debe actualizarse si cambió la entrada.

## 8. Escenario completo RPM

Configure un control con:

- aplicabilidad `APLICABLE`;
- estado `PARCIAL`;
- 30 % de implementación;
- evidencia rechazada o vencida;
- riesgo inherente alto;
- hallazgo recurrente abierto;
- fecha objetivo vencida.

Después:

1. Ejecutar análisis del control.
2. Comprobar antígenos y señales de peligro.
3. Verificar puntaje, prioridad y explicación.
4. Aprobar/modificar/rechazar una decisión desde el rol autorizado.
5. Registrar acción, resultado y efectividad en memoria.
6. Volver a analizar un caso equivalente y comprobar consulta de memoria similar.

## 9. RPM + formación

1. Generar una decisión de tipo `CAPACITACION`.
2. Capacitador: aprobarla y crear el programa asociado.
3. Añadir módulos, materiales y preguntas.
4. Asignar participantes.
5. Registrar respuestas de una evaluación.
6. Verificar cálculo automático del puntaje y estado de aprobación.
7. Emitir y descargar constancia PDF.
8. Confirmar código de verificación.
9. Cerrar sesión y comprobar el código desde el verificador público de la pantalla de acceso; no debe mostrarse el documento de identidad.

## 10. Portal Empresa

1. Entrar con `empresa@demo.com`.
2. Confirmar que solo ve su organización.
3. Revisar etapas, SoA, riesgos, decisiones RPM, formación y reportes.
4. Validar una decisión permitida y comprobar que queda registrada.

## 11. Reportes

1. Descargar PDF y Excel.
2. Confirmar que incluyen resumen, SoA, riesgos y RPM.
3. Verificar que los totales coinciden con los dashboards.

## 12. Seguridad multiempresa

1. Crear dos empresas y usuarios asignados a cada una.
2. Tomar el ID de servicio de la empresa A.
3. Con token de empresa B, intentar consultar SoA, riesgos, evidencia y reporte de A.
4. Esperado: respuesta de acceso denegado/error de negocio sin exposición del contenido.

## 13. Criterios de aceptación

- No hay métricas “quemadas” en frontend.
- Todos los cambios sobreviven al reinicio.
- Los archivos sobreviven al reinicio cuando no se eliminan volúmenes.
- Los roles no acceden a operaciones ajenas.
- RPM explica los factores de la prioridad.
- Ninguna decisión RPM sensible se ejecuta sin validación humana.
- La formación registra resultados e intenta cerrar el ciclo de respuesta.

## 14. Registro de ejecución

No marques un caso como aprobado sin conservar evidencia del resultado real. Registra fallos, bloqueos por configuración y pruebas que aún no se ejecutaron como estados distintos; una prueba no ejecutada no equivale a una prueba aprobada.

## 15. Pruebas solicitadas para la sustentación

Ejecutar primero en una copia de pruebas o *staging*, con datos y cuentas desechables. No cargue malware real ni fuerce fallos sobre producción. Guarde para cada caso la fecha, cuenta de prueba, resultado esperado, respuesta/captura y referencia al log; nunca incluya contraseñas, tokens o enlaces de restablecimiento en las evidencias.

| # | Procedimiento | Resultado seguro esperado |
|---:|---|---|
| 1 | Copiar una URL de pantalla autenticada y abrirla en otro navegador | La URL por sí sola no autentica. El navegador nuevo redirige a inicio de sesión porque el JWT no se transmite en la URL. |
| 2 | Enviar 15 caracteres especiales en el identificador del login | Rechazo genérico de credenciales, sin error 500 ni efecto de consulta; el identificador se trata como dato y no como SQL. |
| 3 | Solicitar restablecimiento | Una vez configurados Brevo y el remitente verificado, el correo de prueba llega; el token expira a los 15 minutos, se guarda como hash y solo sirve una vez. |
| 4 | Fallar el login cinco veces seguidas | La cuenta/identificador queda temporalmente limitado según la ventana configurada y el backend responde 429; probar en una cuenta desechable. |
| 5 | Crear un rol personalizado, asignarlo a dos usuarios y configurar sus capacidades | El mismo rol puede asignarse a dos cuentas; editar el rol cambia sus permisos para ambas. Para permisos distintos por persona, usar una combinación distinta de roles por usuario. |
| 6 | Crear dos usuarios con el mismo correo, incluyendo variación de mayúsculas | La segunda alta se rechaza; el correo se normaliza y la columna tiene restricción única. |
| 7 | Cambiar desde administración la contraseña por un carácter | La política de contraseña rechaza el cambio y no altera la credencial existente. |
| 8 | Asignar dos roles distintos a una cuenta y modificar sus permisos | La cuenta lista ambos roles; las autoridades configuradas se combinan. Al editar roles se actualizan los permisos de todas las cuentas que los usan. |
| 9 | Revisar los logs de auditoría | Deben aparecer cambios de usuarios/roles, actualizaciones de perfil y cierre de sesión; no deben incluir contraseña, token SMTP ni URL de restablecimiento. |
| 10 | Reducir `JWT_EXPIRATION` en entorno de prueba | El token vence tras el tiempo absoluto configurado, aunque haya actividad. |
| 11 | Cambiar `SESSION_IDLE_TIMEOUT_MINUTES` a un valor corto y luego mayor | Con valor corto, una sesión sin solicitudes protegidas expira y se revocan sus tokens; con valor mayor permanece activa hasta superar ese plazo. Restaurar el valor acordado al acabar. |
| 12 | Probar como contraseña de login la secuencia `1` a `15` | Debe fallar para una cuenta donde esa contraseña nunca se haya establecido; la autenticación no convierte esta secuencia en una contraseña válida. |
| 13 | Crear usuario con contraseña `123456789101112131415` | El alta se rechaza por política (falta de variedad y patrón simple). |
| 14 | Escribir `ABC` en contraseña y revisar visualmente el campo | Los caracteres se muestran enmascarados en pantalla. Copiar a otro programa produce el valor real; el portapapeles no es una prueba de enmascaramiento y no debe exigirse que oculte el contenido. |
| 15 | Renombrar un archivo benigno `.exe` de prueba a `.pdf` e intentar subirlo | El backend debe rechazar extensión/tipo/firma no permitidos. Para demostrar ClamAV, usar el archivo de prueba estándar EICAR solo en un entorno de laboratorio aislado y cuando el endpoint ClamAV esté confirmado. |
| 16 | Iniciar sesión con la misma cuenta en dos navegadores | Ambas sesiones pueden coexistir; se registran por separado y cada una vence por inactividad. Cambiar perfil/contraseña o usar cierre global invalida todas. |
| 17 | Generar un error controlado de prueba | El evento debe aparecer en logs del backend de Render, sin datos de autenticación ni secretos en la respuesta pública. No provoque fallos deliberados en producción. |
| 18 | Abrir una segunda pestaña, iniciar sesión por separado, cerrar sesión en la primera y consultar desde la segunda | El logout global incrementa la versión de sesión; la segunda pestaña recibe 401 y vuelve a login. Una URL pegada no copia el token de la primera pestaña. |
| 19 | Inspeccionar en TiDB la columna de contraseña | Debe contener un hash irreversible con BCrypt (prehash SHA-256 + BCrypt), no texto plano ni cifrado recuperable. Para otros datos personales, documentar el cifrado en reposo que provee TiDB/infraestructura; no afirmar que todos los campos estén cifrados por la aplicación. |
| 20 | Actualizar correo o contraseña desde “Mi perfil” | Se requiere la contraseña actual; el correo duplicado y contraseña débil se rechazan. Al aceptar el cambio se registran los logs y se revocan sesiones. |

La migración de sesiones/roles debe aplicarse en TiDB mediante `database/tidb-migrator`, después de verificar backup restaurable y estado `info`/`validate`. No active el código nuevo contra la base de producción antes de completar la migración. La recuperación Brevo también requiere credenciales SMTP y un remitente verificado; siga `docs/CONFIGURAR_BREVO.md`.

## 16. Alcance de verificación

Las pruebas de construcción no sustituyen una validación integrada contra TiDB, Brevo SMTP y ClamAV de producción. Registre por separado lo probado localmente, lo aprobado por CI y lo verificado en servicios externos; no presente como verificado lo que dependa de secretos o servicios aún no configurados.
