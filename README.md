# 🔐 Global ISO Security

Herramienta web para auditoría, capacitación y/o implementación de la norma **ISO 27001**, desarrollada como proyecto académico para la asignatura de Ingeniería Web.

---

## 📋 Descripción del Proyecto

Sistema web que permite gestionar múltiples empresas bajo un modelo multi-tenant lógico, administrar usuarios con control de acceso basado en roles (RBAC), activar checklists dinámicos según el sector económico, registrar evaluaciones de implementación y auditoría, garantizar inmutabilidad de la información una vez firmada, y generar reportes descargables en PDF y Excel.

---

## 👥 Equipo de Desarrollo

| Nombre | Rol en el Proyecto | GitHub |
|---|---|---|
| Andrés Felipe Obando Barriga | Backend (Java + Spring Boot) | @cito515432 |
| Maria Camila Sarmiento | Frontend (HTML + CSS + Bootstrap) | - |
| Juan Esteban Pardo Bedoya | Base de Datos (MySQL + SQL) | - |

**Universidad San Buenaventura — Sede Bogotá**
Programa: Ingeniería de Sistemas — Código SNIES: 2520
Docente: Jairo Armando Salcedo Aranda
Asignatura: Ingeniería Web — 2026

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Frontend | HTML5 + CSS3 + Bootstrap 5 |
| Backend | Java 20 + Spring Boot 3.2.0 |
| Base de Datos | MySQL 8 (XAMPP) |
| Autenticación | JWT (jjwt 0.9.1) |
| Seguridad | Spring Security + BCrypt |
| ORM | Hibernate 6.3.1 / Spring Data JPA |
| Servidor Local | Apache Tomcat (embebido en Spring Boot) |
| Deploy | Railway (pendiente) |
| Control de versiones | Git + GitHub |

---

## 📁 Estructura del Proyecto
```
global-iso-security/
│
├── backend/                        ← Java + Spring Boot
│   └── src/main/java/com/globalisosecurity/backend/
│       ├── controllers/            ← Endpoints REST
│       ├── services/               ← Lógica de negocio
│       ├── models/                 ← Entidades JPA
│       ├── repositories/           ← Acceso a base de datos
│       ├── config/                 ← Seguridad y configuración
│       └── utils/                  ← JWT y utilidades
│
├── frontend/                       ← HTML + CSS + JavaScript
│   ├── pages/                      ← Vistas por rol de usuario
│   ├── css/                        ← Estilos
│   ├── js/                         ← Scripts
│   └── assets/                     ← Imágenes y recursos
│
└── database/                       ← Scripts SQL
    └── schema.sql                  ← Creación de tablas
```

---

## 🔐 Roles del Sistema

| Rol | Tipo | Funciones Principales |
|---|---|---|
| Administrador | Interno | Gestionar usuarios, crear servicios, generar reportes |
| Implementador | Interno | Diligenciar checklist ISO 27001 |
| Auditor | Interno | Validar checklist, firmar auditoría |
| Capacitador | Interno | Acceder a material formativo ISO 27001 |
| Usuario Empresa | Externo | Visualizar servicio, firmar cierre, descargar reportes |

---

## 🚀 Cómo Ejecutar el Proyecto Localmente

### Requisitos previos
- Java JDK 20
- NetBeans IDE (o IntelliJ IDEA)
- XAMPP (MySQL corriendo)
- Maven

### Pasos

**1. Clonar el repositorio**
```bash
git clone https://github.com/cito515432/global-iso-security.git
cd global-iso-security
```

**2. Configurar la base de datos**
- Abrir XAMPP y arrancar MySQL
- Ir a http://localhost/phpmyadmin
- Crear base de datos llamada: `globalisosecurity`

**3. Configurar application.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/globalisosecurity
spring.datasource.username=root
spring.datasource.password=TU_CONTRASEÑA
server.port=8081
```

**4. Ejecutar el backend**
- Abrir la carpeta `backend` en NetBeans
- Clic derecho → Run (F6)
- El servidor arranca en: http://localhost:8081

---

## 📡 Endpoints Disponibles

| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| POST | /api/auth/login | Iniciar sesión | Público |
| GET | /api/usuarios | Listar usuarios | Autenticado |
| POST | /api/usuarios | Crear usuario | Autenticado |
| PUT | /api/usuarios/{id} | Actualizar usuario | Autenticado |
| DELETE | /api/usuarios/{id} | Eliminar usuario | Autenticado |

---

## ✅ Estado del Proyecto

| Módulo | Estado |
|---|---|
| Repositorio GitHub | ✅ Completado |
| Estructura de carpetas | ✅ Completado |
| Backend Spring Boot | ✅ Completado |
| Conexión MySQL | ✅ Completado |
| Autenticación JWT | ✅ Completado |
| Spring Security RBAC | ✅ Completado |
| Frontend HTML/CSS | 🔄 En progreso |
| Base de datos SQL | 🔄 En progreso |
| Endpoints completos | 🔄 En progreso |
| Deploy en Railway | ⏳ Pendiente |

---

## 📄 Documentación

El proyecto incluye documentación completa en la carpeta `docs/`:
- Requisitos funcionales y no funcionales
- Casos de uso
- Diagrama entidad-relación
- Diagrama de arquitectura
- User stories

---

*Última actualización: 18 de Marzo de 2026*
```
# 🔐 Global ISO Security

Herramienta web para auditoría, capacitación y/o implementación de la norma **ISO 27001**, desarrollada como proyecto académico para la asignatura de Ingeniería Web.

---

## 📋 Descripción del Proyecto

Sistema web que permite gestionar múltiples empresas bajo un modelo multi-tenant lógico, administrar usuarios con control de acceso basado en roles (RBAC), activar checklists dinámicos según el sector económico, registrar evaluaciones de implementación y auditoría, garantizar inmutabilidad de la información una vez firmada, y generar reportes descargables en PDF y Excel.

---

## 👥 Equipo de Desarrollo

| Nombre                       | Rol en el Proyecto                | GitHub      |
| ---------------------------- | --------------------------------- | ----------- |
| Andrés Felipe Obando Barriga | Backend (Java + Spring Boot)      | @cito515432 |
| Maria Camila Sarmiento       | Frontend (HTML + CSS + Bootstrap) | -           |
| Juan Esteban Pardo Bedoya    | Base de Datos (MySQL + SQL)       | -           |

**Universidad San Buenaventura — Sede Bogotá**
Programa: Ingeniería de Sistemas — Código SNIES: 2520
Docente: Jairo Armando Salcedo Aranda
Asignatura: Ingeniería Web — 2026

---

## 🛠️ Stack Tecnológico

| Capa                 | Tecnología                              |
| -------------------- | --------------------------------------- |
| Frontend             | HTML5 + CSS3 + Bootstrap 5              |
| Backend              | Java 20 + Spring Boot 3.2.0             |
| Base de Datos        | MySQL 8 (XAMPP)                         |
| Autenticación        | JWT (jjwt 0.9.1)                        |
| Seguridad            | Spring Security + BCrypt                |
| ORM                  | Hibernate 6 / Spring Data JPA           |
| Servidor Local       | Apache Tomcat (embebido en Spring Boot) |
| Deploy               | Railway (pendiente)                     |
| Control de versiones | Git + GitHub                            |

---

## 📁 Estructura del Proyecto

```
global-iso-security/
│
├── backend/
│   └── src/main/java/com/globalisosecurity/backend/
│       ├── controllers/
│       ├── services/
│       ├── models/
│       ├── repositories/
│       ├── config/
│       └── utils/
│
├── frontend/
│   ├── pages/
│   ├── css/
│   ├── js/
│   └── assets/
│
└── database/
    └── schema.sql
```

---

## 🔐 Roles del Sistema

| Rol             | Tipo    | Funciones Principales                                  |
| --------------- | ------- | ------------------------------------------------------ |
| Administrador   | Interno | Gestionar usuarios, crear servicios, generar reportes  |
| Implementador   | Interno | Diligenciar checklist ISO 27001                        |
| Auditor         | Interno | Validar checklist, firmar auditoría                    |
| Capacitador     | Interno | Acceder a material formativo ISO 27001                 |
| Usuario Empresa | Externo | Visualizar servicio, firmar cierre, descargar reportes |

---

## 🚀 Cómo Ejecutar el Proyecto Localmente

### Requisitos previos

* Java JDK 20
* NetBeans IDE (o IntelliJ IDEA)
* XAMPP (MySQL corriendo)
* Maven

### Pasos

**1. Clonar el repositorio**

```bash
git clone https://github.com/cito515432/global-iso-security.git
cd global-iso-security
```

**2. Configurar la base de datos**

* Abrir XAMPP y arrancar MySQL
* Ir a http://localhost/phpmyadmin
* Crear base de datos: `globalisosecurity`

**3. Configurar application.properties**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/globalisosecurity
spring.datasource.username=root
spring.datasource.password=TU_CONTRASEÑA
server.port=8081
```

**4. Ejecutar el backend**

* Abrir `backend` en NetBeans
* Run (F6)
* Servidor en: http://localhost:8081

---

## 📡 Endpoints Disponibles

### 🔐 Autenticación

| Método | Endpoint        |
| ------ | --------------- |
| POST   | /api/auth/login |

### 👤 Usuarios

| Método | Endpoint           |
| ------ | ------------------ |
| GET    | /api/usuarios      |
| POST   | /api/usuarios      |
| PUT    | /api/usuarios/{id} |
| DELETE | /api/usuarios/{id} |

### 🏢 Empresas

| Método | Endpoint           |
| ------ | ------------------ |
| GET    | /api/empresas      |
| POST   | /api/empresas      |
| PUT    | /api/empresas/{id} |
| DELETE | /api/empresas/{id} |

### 🏭 Sectores

| Método | Endpoint           |
| ------ | ------------------ |
| GET    | /api/sectores      |
| POST   | /api/sectores      |
| PUT    | /api/sectores/{id} |
| DELETE | /api/sectores/{id} |

### 📊 Servicios

| Método | Endpoint            |
| ------ | ------------------- |
| GET    | /api/servicios      |
| POST   | /api/servicios      |
| PUT    | /api/servicios/{id} |
| DELETE | /api/servicios/{id} |

---

## 🧪 Pruebas realizadas

* ✔ Pruebas con Postman (Desktop)
* ✔ Creación de registros en MySQL
* ✔ Persistencia de datos verificada
* ✔ Endpoints REST funcionando
* ✔ Manejo de errores 403 / 401
* ✔ Seguridad temporal configurada para desarrollo

---

## ✅ Estado del Proyecto

| Módulo                 | Estado |
| ---------------------- | ------ |
| Repositorio GitHub     | ✅      |
| Estructura de carpetas | ✅      |
| Backend Spring Boot    | ✅      |
| Conexión MySQL         | ✅      |
| CRUD Usuarios          | ✅      |
| CRUD Empresas          | ✅      |
| CRUD Sectores          | ✅      |
| CRUD Servicios         | ✅      |
| Seguridad (temporal)   | ✅      |
| Frontend               | 🔄     |
| Validaciones           | ⏳      |
| Reglas de negocio      | ⏳      |
| Deploy                 | ⏳      |

---

## 📄 Documentación

* Requisitos funcionales
* Casos de uso
* Modelo entidad-relación
* Arquitectura del sistema

---

## 🚧 Próximos pasos

* Validaciones (duplicados, campos obligatorios)
* Reglas de negocio (estados de servicio)
* Implementación completa de JWT
* Desarrollo del frontend
* Generación de reportes (PDF / Excel)
* Deploy en Railway

---

*Última actualización: 24 de Marzo de 2026*
# 🔐 Global ISO Security

Herramienta web para auditoría, capacitación y/o implementación de la norma **ISO 27001**, desarrollada como proyecto académico para la asignatura de **Ingeniería Web**.

---

## 📋 Descripción del Proyecto

**Global ISO Security** es un sistema web diseñado para gestionar procesos de auditoría, capacitación e implementación de controles relacionados con **ISO 27001**. El sistema permite administrar múltiples empresas, usuarios con diferentes roles, servicios, checklists, evaluaciones, firmas y trazabilidad mediante logs de auditoría.

El objetivo del proyecto es ofrecer una plataforma que apoye el seguimiento estructurado de procesos de seguridad de la información, permitiendo registrar avances, validar cumplimiento y mantener evidencia del proceso realizado.

---

## 👥 Equipo de Desarrollo

| Nombre                       | Rol en el Proyecto                | GitHub      |
| ---------------------------- | --------------------------------- | ----------- |
| Andrés Felipe Obando Barriga | Backend (Java + Spring Boot)      | @cito515432 |
| Maria Camila Sarmiento       | Frontend (HTML + CSS + Bootstrap) | -           |
| Juan Esteban Pardo Bedoya    | Base de Datos (MySQL + SQL)       | -           |

**Universidad San Buenaventura – Sede Bogotá**
**Programa:** Ingeniería de Sistemas
**Asignatura:** Ingeniería Web
**Docente:** Jairo Armando Salcedo Aranda

---

## 🛠️ Stack Tecnológico

| Capa                 | Tecnología                  |
| -------------------- | --------------------------- |
| Frontend             | HTML5 + CSS3 + Bootstrap 5  |
| Backend              | Java 20 + Spring Boot 3.2.0 |
| Base de Datos        | MySQL 8 (XAMPP)             |
| ORM                  | Spring Data JPA + Hibernate |
| Seguridad            | Spring Security + JWT       |
| Cifrado              | BCrypt                      |
| Pruebas API          | Postman                     |
| Control de versiones | Git + GitHub                |
| IDE Backend          | NetBeans                    |
| Deploy               | Railway (pendiente)         |

---

## 📁 Estructura del Proyecto

```text
global-iso-security/
│
├── backend/
│   └── src/main/java/com/globalisosecurity/backend/
│       ├── config/
│       ├── controllers/
│       ├── exceptions/
│       ├── models/
│       ├── repositories/
│       ├── services/
│       └── utils/
│
├── frontend/
│   ├── pages/
│   ├── css/
│   ├── js/
│   └── assets/
│
└── database/
    └── schema.sql
```

---

## 🔐 Roles del Sistema

| Rol             | Tipo    | Funciones Principales                              |
| --------------- | ------- | -------------------------------------------------- |
| Administrador   | Interno | Gestionar usuarios, empresas, servicios y reportes |
| Implementador   | Interno | Diligenciar checklist y registrar avances          |
| Auditor         | Interno | Evaluar cumplimiento y validar procesos            |
| Capacitador     | Interno | Gestionar actividades de formación                 |
| Usuario Empresa | Externo | Consultar estado del servicio y firmar procesos    |

---

## 🚀 Funcionalidades Implementadas en Backend

Actualmente el backend incluye los siguientes módulos:

### ✅ Autenticación y Seguridad

* Login con **JWT**
* Generación de token
* Validación de token
* Protección de rutas con `Bearer Token`
* Spring Security configurado

### ✅ Gestión de Usuarios

* Crear usuario
* Listar usuarios
* Actualizar usuario
* Eliminar usuario

### ✅ Gestión de Empresas

* CRUD completo
* Validación de nombre obligatorio
* Validación de empresas duplicadas

### ✅ Gestión de Sectores

* CRUD completo
* Validación de nombre obligatorio
* Validación de sectores duplicados

### ✅ Gestión de Servicios

* CRUD completo
* Asociación con empresa y sector
* Validación de estados:

  * `BORRADOR`
  * `EN_PROCESO`
  * `FINALIZADO`
  * `FIRMADO`
  * `CERRADO`
* Bloqueo de edición/eliminación en estados finales

### ✅ Checklists

* CRUD de checklist
* Asociación con servicio
* Estados:

  * `PENDIENTE`
  * `EN_PROCESO`
  * `COMPLETADO`

### ✅ Ítems del Checklist

* CRUD de ítems
* Asociación con checklist
* Estados:

  * `PENDIENTE`
  * `CUMPLE`
  * `NO_CUMPLE`
  * `EN_PROCESO`

### ✅ Evaluaciones

* CRUD de evaluaciones
* Asociación con servicio
* Estados:

  * `PENDIENTE`
  * `EN_REVISION`
  * `APROBADA`
  * `RECHAZADA`

### ✅ Firmas

* CRUD de firmas
* Asociación con servicio
* Estados:

  * `PENDIENTE`
  * `FIRMADA`
  * `RECHAZADA`

### ✅ Logs de Auditoría

* Registro manual y automático de eventos
* Consulta por:

  * usuario
  * acción
  * módulo

### ✅ Manejo de Errores

* Excepciones personalizadas
* Respuestas más limpias con:

  * `400 Bad Request`
  * `404 Not Found`
  * `500 Internal Server Error`

---

## 📡 Endpoints Principales

### Autenticación

* `POST /api/auth/login`

### Usuarios

* `GET /api/usuarios`
* `POST /api/usuarios`
* `PUT /api/usuarios/{id}`
* `DELETE /api/usuarios/{id}`

### Empresas

* `GET /api/empresas`
* `POST /api/empresas`
* `PUT /api/empresas/{id}`
* `DELETE /api/empresas/{id}`

### Sectores

* `GET /api/sectores`
* `POST /api/sectores`
* `PUT /api/sectores/{id}`
* `DELETE /api/sectores/{id}`

### Servicios

* `GET /api/servicios`
* `POST /api/servicios`
* `PUT /api/servicios/{id}`
* `DELETE /api/servicios/{id}`

### Checklists

* `GET /api/checklists`
* `POST /api/checklists`
* `PUT /api/checklists/{id}`
* `DELETE /api/checklists/{id}`

### Ítems del Checklist

* `GET /api/items-checklist`
* `POST /api/items-checklist`
* `PUT /api/items-checklist/{id}`
* `DELETE /api/items-checklist/{id}`

### Evaluaciones

* `GET /api/evaluaciones`
* `POST /api/evaluaciones`
* `PUT /api/evaluaciones/{id}`
* `DELETE /api/evaluaciones/{id}`

### Firmas

* `GET /api/firmas`
* `POST /api/firmas`
* `PUT /api/firmas/{id}`
* `DELETE /api/firmas/{id}`

### Logs de Auditoría

* `GET /api/logs-auditoria`
* `POST /api/logs-auditoria`
* `DELETE /api/logs-auditoria/{id}`

---

## 🧪 Estado Actual del Proyecto

| Módulo                       | Estado         |
| ---------------------------- | -------------- |
| Repositorio GitHub           | ✅ Completado   |
| Estructura del proyecto      | ✅ Completado   |
| Backend Spring Boot          | ✅ Completado   |
| Conexión MySQL               | ✅ Completado   |
| Seguridad con JWT            | ✅ Completado   |
| CRUD Usuarios                | ✅ Completado   |
| CRUD Empresas                | ✅ Completado   |
| CRUD Sectores                | ✅ Completado   |
| CRUD Servicios               | ✅ Completado   |
| Checklists                   | ✅ Completado   |
| Ítems del Checklist          | ✅ Completado   |
| Evaluaciones                 | ✅ Completado   |
| Firmas                       | ✅ Completado   |
| Logs de auditoría            | ✅ Completado   |
| Frontend                     | 🔄 En progreso |
| Integración frontend-backend | ⏳ Pendiente    |
| Deploy                       | ⏳ Pendiente    |

---

## ▶️ Cómo Ejecutar el Proyecto Localmente

### Requisitos previos

* Java JDK 20
* NetBeans
* XAMPP
* MySQL
* Maven

### Pasos

#### 1. Clonar el repositorio

```bash
git clone https://github.com/cito515432/global-iso-security.git
cd global-iso-security
```

#### 2. Crear la base de datos

Abrir XAMPP y luego entrar a `http://localhost/phpmyadmin`
Crear una base de datos llamada:

```text
globalisosecurity
```

#### 3. Configurar `application.properties`

Ejemplo:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/globalisosecurity
spring.datasource.username=root
spring.datasource.password=TU_CONTRASEÑA
server.port=8081
```

#### 4. Ejecutar el backend

Abrir la carpeta `backend` en NetBeans y ejecutar con:

```text
Run (F6)
```

#### 5. Probar login en Postman

Endpoint:

```http
POST /api/auth/login
```

Body:

```json
{
  "email": "admin@globalisosecurity.com",
  "password": "123456"
}
```

Luego usar el token en rutas protegidas:

```text
Authorization: Bearer TU_TOKEN
```

---

## 🧾 Próximos Pasos

* Mejorar logs automáticos con usuario real e IP real
* Reforzar reglas de negocio finales
* Integrar frontend con backend
* Generar reportes PDF/Excel
* Preparar despliegue en Railway
* Documentación final del proyecto

---

## 📌 Estado General

El backend del proyecto se encuentra en una fase **muy avanzada**, con la mayoría de los módulos principales ya implementados, protegidos y probados. Actualmente el enfoque principal pasa a ser la integración con el frontend, refinamiento de reglas de negocio y preparación de la entrega final.

---
# 📄 Informe de Desarrollo – Frontend  
## 🔐 Global ISO Security

**Fecha:** 28 de marzo de 2026  

---

## 📌 Objetivo de la sesión

Continuar el desarrollo del frontend del sistema, incorporando un dashboard funcional, mejorando la estructura del panel administrador e implementando la sección de configuración con parámetros del sistema, seguridad y flujo de servicios.

---

## 🧩 Desarrollo realizado

### 📊 1. Dashboard

Se implementó una vista principal para el sistema que permite visualizar el estado general:

- Indicadores de:
  - Usuarios
  - Empresas
  - Servicios
  - Auditorías
- Estados de servicios:
  - En proceso
  - Firmados
  - Cerrados
- Panel de resumen del sistema

✔️ Permite una visión general del estado operativo

---

### 👨‍💼 2. Panel Administrador

Se consolidó el panel principal del administrador con los siguientes módulos:

- Gestión de usuarios
- Empresas
- Servicios
- Reportes
- Configuración

Mejoras realizadas:

- Navegación más clara
- Organización por secciones
- Eliminación del módulo independiente de logs para simplificar la interfaz

---

### 👥 3. Gestión de Usuarios

- Visualización de usuarios con:
  - Nombre
  - Email
  - Rol
  - Empresa
  - Estado
- Acciones:
  - Editar
  - Eliminar
- Roles manejados:
  - Administrador
  - Implementador
  - Auditor
  - Capacitador

---

### 🏢 4. Empresas

- Registro y gestión de empresas
- Asociación con servicios
- Clasificación por sector económico

✔️ Soporte para modelo multi-tenant

---

### 📋 5. Servicios

Se estructuró el módulo central del sistema:

#### Estados del servicio:
- BORRADOR
- EN PROCESO
- FIRMADO
- CERRADO

#### Roles asociados:
- Implementador
- Capacitador
- Auditor

✔️ Alineado con el flujo del proyecto ISO 27001

---

### 📊 6. Reportes

- Generación de:
  - PDF
  - Excel
- Visualización de logs desde este módulo

✔️ Preparado para integración con backend

---

### ⚙️ 7. Configuración del Sistema

Se implementó una nueva sección de configuración con:

#### 🔹 Información del sistema
- Nombre del sistema
- Versión
- Correo de soporte
- Estado del sistema

#### 🔹 Seguridad
- Duración de sesión
- Intentos máximos de login
- Bloqueo por intentos fallidos
- Cierre automático por inactividad

#### 🔹 Flujo del servicio
- Edición permitida en:
  - BORRADOR
  - EN PROCESO
- Bloqueo en:
  - FIRMADO
  - CERRADO

#### 🔹 Reportes
- Exportación en PDF y Excel
- Inclusión de logo institucional
- Restricción a servicios cerrados

✔️ Basado en buenas prácticas de seguridad ISO 27001

---

## 📊 Estado actual

| Módulo | Estado |
|------|--------|
| Login | ✅ Completado |
| Dashboard | ✅ Completado |
| Administrador | ✅ Completado |
| Implementador | ✅ Completado |
| Configuración | ✅ Completado |
| Frontend general | 🔄 En integración |
| Backend | 🔄 En progreso |
| Base de datos | 🔄 En progreso |
| Seguridad JWT | 🔄 En integración |

---

## 🚀 Próximos pasos

- Integrar frontend con backend (Spring Boot)
- Implementar autenticación JWT
- Conectar con base de datos MySQL
- Implementar lógica real de checklist ISO 27001
- Generación dinámica de reportes

---

## 👥 Equipo de desarrollo

- Andrés Felipe Obando Barriga – Backend (Java + Spring Boot)  
- María Camila Sarmiento – Frontend (HTML + CSS + Bootstrap)  
- Juan Esteban Pardo Bedoya – Base de Datos (MySQL + SQL)  

---

## 🛠️ Stack tecnológico

- **Frontend:** HTML5, CSS3, Bootstrap 5  
- **Backend:** Java 20, Spring Boot  
- **Base de datos:** MySQL (XAMPP)  
- **Seguridad:** JWT, Spring Security  
- **Control de versiones:** Git + GitHub  

---

## 📌 Conclusión

El sistema presenta una interfaz sólida, organizada y coherente con los objetivos del proyecto.  
Se logró estructurar correctamente el panel administrativo, implementar el dashboard y definir la configuración del sistema.

El siguiente paso clave es la integración completa con el backend y la lógica de negocio.
