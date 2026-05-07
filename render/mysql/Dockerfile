# MySQL para Render Private Service.
# Importa automáticamente los archivos .sql de database/init/ cuando el disco /var/lib/mysql está vacío.

FROM mysql:8.0

COPY database/init/ /docker-entrypoint-initdb.d/

CMD ["mysqld", "--default-authentication-plugin=mysql_native_password"]
