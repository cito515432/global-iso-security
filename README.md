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
