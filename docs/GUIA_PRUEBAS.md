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

## 14. Pruebas no ejecutadas en el entorno de construcción

La entrega fue compilada estáticamente y el JavaScript fue validado, pero este entorno no dispone de Docker/MySQL. Por ello, las pruebas descritas aquí deben ejecutarse en la máquina de desarrollo del equipo y guardar capturas, logs, videos y resultados para el Capítulo 2 y los anexos de la tesis.
