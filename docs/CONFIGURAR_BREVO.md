# Configurar restablecimiento por correo con Brevo

El backend ya usa JavaMail SMTP; no guarda claves de restablecimiento en texto plano. Para habilitar el envío en Render, configura las variables del servicio `globaliso-backend-cito515432` desde el panel de Render:

| Variable | Valor |
|---|---|
| `SMTP_HOST` | `smtp-relay.brevo.com` |
| `SMTP_PORT` | `587` |
| `SMTP_USERNAME` | Login SMTP mostrado en Brevo, no el host |
| `SMTP_PASSWORD` | Clave SMTP de Brevo, no la contraseña de la cuenta ni una clave API |
| `PASSWORD_RESET_FROM` | Remitente verificado en Brevo |
| `PASSWORD_RESET_ENABLED` | `true`, después de probar las credenciales SMTP y el remitente |

Brevo documenta `smtp-relay.brevo.com` y admite el puerto 587; el nombre de usuario y la clave SMTP se consultan en **Configuración → SMTP y API**. Usa el login que muestra ese panel; puede diferir del correo con el que inicias sesión. La clave SMTP no es una API key. Consulta la [guía de envío SMTP de Brevo](https://help.brevo.com/hc/es/articles/7924908994450-Enviar-emails-transaccionales-con-Brevo-SMTP) y sus [pasos de solución de problemas](https://help.brevo.com/hc/es/articles/115000188150-Soluci%C3%B3n-de-problemas-relacionados-con-Brevo-SMTP).

Mantén `SMTP_USERNAME`, `SMTP_PASSWORD` y `PASSWORD_RESET_FROM` como valores protegidos en Render. No los pongas en el repositorio, en capturas de la tesis ni en el chat. El endpoint de restablecimiento permanece deshabilitado hasta que esas variables estén completas. Después de configurarlas, despliega el backend, solicita un restablecimiento desde una cuenta de prueba y confirma la recepción del mensaje, expiración a los 15 minutos, uso único del enlace y rechazo de contraseñas que no cumplan la política.
