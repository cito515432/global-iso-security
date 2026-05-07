# MySQL en Render

Este servicio usa la imagen oficial de MySQL 8 y copia los scripts de `database/init/`.
El archivo `01_globalisosecurity_backup.sql` se importa solo la primera vez que el disco persistente `/var/lib/mysql` está vacío.

Si ya existe un disco con datos, MySQL no vuelve a importar automáticamente el SQL. En ese caso se debe importar manualmente o recrear el disco/servicio.
